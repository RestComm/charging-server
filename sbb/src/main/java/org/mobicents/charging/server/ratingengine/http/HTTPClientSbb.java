/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2013, TeleStax and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for
 * a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.charging.server.ratingengine.http;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.java.client.slee.resource.http.HttpClientActivity;
import net.java.client.slee.resource.http.HttpClientActivityContextInterfaceFactory;
import net.java.client.slee.resource.http.HttpClientResourceAdaptorSbbInterface;
import net.java.client.slee.resource.http.event.ResponseEvent;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.ratingengine.RatingEngineClient;
import org.mobicents.charging.server.ratingengine.RatingInfo;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This is a simple service for testing HTTP
 * 
 * @author rsaranathan
 * 
 */
public abstract class HTTPClientSbb extends BaseSbb implements Sbb, RatingEngineClient {

	private Tracer tracer;

	private SbbContext sbbContext; // This SBB's SbbContext

	private HttpClientActivityContextInterfaceFactory httpClientAci;

	private HttpClientResourceAdaptorSbbInterface raSbbInterface;

	private String httpURLString;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
	 */
	public void setSbbContext(SbbContext context) {

		this.sbbContext = context;
		this.tracer = sbbContext.getTracer("CS-REClient-HTTP");

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");

			httpClientAci = (HttpClientActivityContextInterfaceFactory) ctx.lookup("slee/resources/http-client/acifactory");

			raSbbInterface = (HttpClientResourceAdaptorSbbInterface) ctx.lookup("slee/resources/http-client/sbbinterface");

			httpURLString = (String) ctx.lookup("HTTPURL");
		}
		catch (NamingException ne) {
			tracer.severe("Could not set SBB context:", ne);
		}

	}

	// CMP Fields

	public abstract void setFeedHashCode(int feedHashCode);

	public abstract int getFeedHashCode();

	// Event handler methods
	public void onResponseEvent(ResponseEvent event, ActivityContextInterface aci) {
		HttpResponse response = event.getHttpResponse();
		tracer.info("********** onResponseEvent **************");
		tracer.info("URI = " + event.getRequestApplicationData());
		tracer.info("Status Code = " + response.getStatusLine().getStatusCode());
		try {
			tracer.info("Response Body = " + EntityUtils.toString(response.getEntity()));
		}
		catch (Exception e) {
			tracer.severe("Failed reading response body", e);
		}
		tracer.info("*****************************************");
	}

	//----------------------- HTTP Implementation ------------------------------------//
	@SuppressWarnings({ "rawtypes" })
	public RatingInfo getRateForService(HashMap params){
		String sessionIdFromRequest = params.get("SessionId").toString();
		try {
			HttpPost httpPost = new HttpPost(httpURLString);

			try {

				HttpClient client = raSbbInterface.getHttpClient();
				HttpClientActivity clientActivity = raSbbInterface.createHttpClientActivity(true, null);

				ActivityContextInterface clientAci = httpClientAci.getActivityContextInterface(clientActivity);
				clientAci.attach(sbbContext.getSbbLocalObject());

				long bmStart = System.currentTimeMillis();
				tracer.info("------ HTTP Request Params to Rating Engine ------");
				String httpRequestParams = "";
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());

				for (Object o: params.entrySet()) {
					Map.Entry entry = (Map.Entry) o;
					String key = null;
					String val = null;
					if(entry.getKey() != null){
						key = entry.getKey().toString();
					}
					if(entry.getValue() != null){
						val = entry.getValue().toString();
					}
					if(key==null || val==null){
						continue;
					}
					nameValuePairs.add(new BasicNameValuePair(key, val));
					httpRequestParams += key+"="+val+"; ";
				}
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				tracer.info(httpRequestParams);
				//Synchronous
				HttpResponse response = client.execute(httpPost);
				tracer.info("------ HTTP Synchronous Response from Rating Engine ------");
				tracer.info("Response from Rating Engine took " + (System.currentTimeMillis()-bmStart) + " milliseconds.");
				//tracer.info("URI = " + httpURLString);
				//tracer.info("Response Headers = " + response.toString());
				//tracer.info("HTTP Status Code = " + response.getStatusLine().getStatusCode());
				String responseBody = "";
				try {
					responseBody = EntityUtils.toString(response.getEntity());
				} catch (Exception e) {
					tracer.severe("Failed reading response body", e);
				}
				//tracer.info("Response Body = " + responseBody); 

				// The response body is an XML payload. Let's parse it using DOM.
				int responseCode = -1;
				String sessionId = sessionIdFromRequest;
				long actualTime = 0;
				long currentTime = 0;
				double rate = 0.0D;
				String rateDescription = "";
				String ratePromo = "";
				try {
					DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(responseBody));
					Document doc = db.parse(is);

					NodeList nodes = doc.getElementsByTagName("response");
					Element element = (Element) nodes.item(0);

					responseCode = Integer.parseInt(getCharacterDataFromElement((Element) element.getElementsByTagName("responseCode").item(0)));
					sessionId = getCharacterDataFromElement((Element) element.getElementsByTagName("sessionId").item(0));
					if(sessionIdFromRequest != sessionId){
						tracer.warning("SessionID Mismatch! Something is wrong with the response from the Rating Engine. Expected '"+sessionIdFromRequest+"', received '"+sessionId+"'");
					}
					actualTime = Long.parseLong(getCharacterDataFromElement((Element) element.getElementsByTagName("actualTime").item(0)));
					currentTime = Long.parseLong(getCharacterDataFromElement((Element) element.getElementsByTagName("currentTime").item(0)));
					rate = Double.parseDouble(getCharacterDataFromElement((Element) element.getElementsByTagName("rate").item(0)));
					rateDescription = getCharacterDataFromElement((Element) element.getElementsByTagName("rateDescription").item(0));
					ratePromo = getCharacterDataFromElement((Element) element.getElementsByTagName("ratePromo").item(0));

					tracer.info(
							"responseCode="+responseCode+"; "+
							"sessionId="+sessionId+"; "+
							"actualTime="+actualTime+"; "+
							"currentTime="+currentTime+"; "+
							"rate="+rate+"; "+
							"rateDescription="+rateDescription+"; "+
							"ratePromo="+ratePromo);

				} catch (Exception e) {
					tracer.warning("Malformed response from Rating Engine for request:\n"+httpRequestParams+"\n\nResponse Received was:"+responseBody, e);
				}

				return new RatingInfo(responseCode, sessionId, actualTime, currentTime, rate, rateDescription, ratePromo);
				//Asynchronous
				//clientActivity.execute(httpPost, httpURLString);

			} catch (Throwable e) {
				tracer.severe("Error while creating HttpClientActivity", e);
			}
		} catch (Exception e) {
			tracer.severe("Failed to process HTTP request", e);
		}
		return new RatingInfo(-1, sessionIdFromRequest);
	}

	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "?";
	}

}
