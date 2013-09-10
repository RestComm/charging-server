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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;
import javax.slee.resource.StartActivityException;
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
import org.mobicents.charging.server.DiameterChargingServer;
import org.mobicents.charging.server.ratingengine.RatingEngineClient;
import org.mobicents.charging.server.ratingengine.RatingInfo;
import org.mobicents.slee.SbbContextExt;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * SBB for Rating Engine Client implementation in HTTP
 * 
 * @author rsaranathan
 * @author ammendonca
 * 
 */
public abstract class HTTPClientSbb extends BaseSbb implements Sbb, RatingEngineClient {

	private Tracer tracer;

	private SbbContextExt sbbContext; // This SBB's SbbContext

	private HttpClientActivityContextInterfaceFactory httpClientAci;

	private HttpClientResourceAdaptorSbbInterface raSbbInterface;

	private String httpURLString;

	private boolean sync = true;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
	 */
	public void setSbbContext(SbbContext context) {

		this.sbbContext = (SbbContextExt) context;
		this.tracer = sbbContext.getTracer("CS-RF-HTTP");

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

	// Event handler methods
	public void onResponseEvent(ResponseEvent event, ActivityContextInterface aci) {
		HttpResponse response = event.getHttpResponse();
		if (tracer.isInfoEnabled()) {
			tracer.info("[<<] Received HTTP Response. Status Code = " + response.getStatusLine().getStatusCode());
			if (tracer.isFineEnabled()) {
				try {
					tracer.fine("[<<] Received HTTP Response. Response Body = [" + EntityUtils.toString(response.getEntity()) + "]");
				}
				catch (Exception e) {
					tracer.severe("[xx] Failed reading response body", e);
				}
			}
		}

		// end http activity
		((HttpClientActivity) aci.getActivity()).endActivity();

		// call back parent
		HashMap params = (HashMap) event.getRequestApplicationData();
		RatingInfo ratInfo = buildRatingInfo(response, params);
		final DiameterChargingServer parent = (DiameterChargingServer) sbbContext.getSbbLocalObject().getParent();
		parent.getRateForServiceResult(ratInfo);
	}

	//----------------------- HTTP Implementation ------------------------------------//

	public RatingInfo getRateForService(HashMap params) {
		if (sync) {
			return getRateForServiceSync(params);
		}
		else {
			return getRateForServiceAsync(params);
		}
	}

	public RatingInfo getRateForServiceSync(HashMap params) {
		String sessionIdFromRequest = params.get("SessionId").toString();
		HttpClient client = raSbbInterface.getHttpClient();

		long bmStart = System.currentTimeMillis();
		HttpPost httpPost = buildHTTPRequest(params);

		// Synchronous call
		HttpResponse response = null;
		try {
			tracer.info("[>>] Sending HTTP Request to Rating Client in synchronous mode.");
			response = client.execute(httpPost);
		}
		catch (IOException e) {
			tracer.severe("[xx] Failed to send HTTP Request to Rating Engine.");
			return new RatingInfo(-1, sessionIdFromRequest);
		}
		tracer.info("[%%] Response from Rating Engine took " + (System.currentTimeMillis() - bmStart) + " milliseconds.");

		return buildRatingInfo(response, params);
	}

	public RatingInfo getRateForServiceAsync(HashMap params) {
		String sessionIdFromRequest = params.get("SessionId").toString();

		HttpClientActivity clientActivity = null;
		try {
			clientActivity = raSbbInterface.createHttpClientActivity(true, null);
		} catch (StartActivityException e) {
			tracer.severe("[xx] Failed creating HTTP Client Activity to send HTTP Request to Rating Engine.");
			return new RatingInfo(-1, sessionIdFromRequest);
		}

		ActivityContextInterface clientAci = httpClientAci.getActivityContextInterface(clientActivity);
		clientAci.attach(sbbContext.getSbbLocalObject());

		params.put("startTime", System.currentTimeMillis());
		HttpPost httpPost = buildHTTPRequest(params);

		// Asynchronous call
		clientActivity.execute(httpPost, params);
		tracer.info("[>>] Sent HTTP Request to Rating Client in asynchronous mode.");

		return null;
	}

	private HttpPost buildHTTPRequest(HashMap params) {
		HttpPost httpPost = new HttpPost(httpURLString);

		tracer.info("------ HTTP Request Params to Rating Engine ------");
		String httpRequestParams = "";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());

		for (Object o: params.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			String key = null;
			String val = null;
			if (entry.getKey() != null) {
				key = entry.getKey().toString();
			}
			if (entry.getValue() != null) {
				val = entry.getValue().toString();
			}
			if (key==null || val==null) {
				continue;
			}
			nameValuePairs.add(new BasicNameValuePair(key, val));
			httpRequestParams += key + "=" + val + "; ";
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		tracer.info(httpRequestParams);

		return httpPost;
	}

	private RatingInfo buildRatingInfo(HttpResponse response, HashMap params) {
		String responseBody = "";

		String diameterSessionId = (String) params.get("SessionId");
		try {
			responseBody = EntityUtils.toString(response.getEntity());
		}
		catch (Exception e) {
			tracer.severe("[xx] Failed reading HTTP Rating Engine response body.", e);
			return new RatingInfo(-1, diameterSessionId);
		}
		//tracer.info("Response Body = " + responseBody);

		// The response body is an XML payload. Let's parse it using DOM.
		int responseCode = -1;
		String sessionId = diameterSessionId;
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
			if (!diameterSessionId.equals(sessionId)){
				tracer.warning("SessionID Mismatch! Something is wrong with the response from the Rating Engine. Expected '" + diameterSessionId + "', received '"+sessionId+"'");
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

		}
		catch (Exception e) {
			tracer.warning("[xx] Malformed response from Rating Engine for request:\n" + params + "\n\nResponse Received was:"+responseBody, e);
			return new RatingInfo(-1, diameterSessionId);
		}

		return new RatingInfo(responseCode, sessionId, actualTime, currentTime, rate, rateDescription, ratePromo);
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
