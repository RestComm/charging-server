/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, TeleStax and individual contributors as indicated
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

package org.mobicents.charging.server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.InitialEventSelector;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TimerPreserveMissed;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.serviceactivity.ServiceStartedEvent;

import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterAvpType;
import net.java.slee.resource.diameter.base.events.avp.DiameterResultCode;
import net.java.slee.resource.diameter.base.events.avp.GroupedAvp;
import net.java.slee.resource.diameter.cca.events.avp.CcRequestType;
import net.java.slee.resource.diameter.cca.events.avp.CcUnitType;
import net.java.slee.resource.diameter.cca.events.avp.CreditControlResultCode;
import net.java.slee.resource.diameter.cca.events.avp.FinalUnitActionType;
import net.java.slee.resource.diameter.cca.events.avp.FinalUnitIndicationAvp;
import net.java.slee.resource.diameter.cca.events.avp.GrantedServiceUnitAvp;
import net.java.slee.resource.diameter.cca.events.avp.MultipleServicesCreditControlAvp;
import net.java.slee.resource.diameter.cca.events.avp.RequestedActionType;
import net.java.slee.resource.diameter.cca.events.avp.RequestedServiceUnitAvp;
import net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdAvp;
import net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType;
import net.java.slee.resource.diameter.cca.events.avp.UsedServiceUnitAvp;
import net.java.slee.resource.diameter.ro.RoAvpFactory;
import net.java.slee.resource.diameter.ro.RoProvider;
import net.java.slee.resource.diameter.ro.RoServerSessionActivity;
import net.java.slee.resource.diameter.ro.events.RoCreditControlAnswer;
import net.java.slee.resource.diameter.ro.events.RoCreditControlRequest;

import org.mobicents.charging.server.account.AccountBalanceManagement;
import org.mobicents.charging.server.account.CreditControlInfo;
import org.mobicents.charging.server.account.CreditControlInfo.ErrorCodeType;
import org.mobicents.charging.server.account.CreditControlUnit;
import org.mobicents.charging.server.cdr.CDRGenerator;
import org.mobicents.charging.server.ratingengine.RatingEngineClient;
import org.mobicents.charging.server.ratingengine.RatingInfo;
import org.mobicents.charging.server.data.DataSource;
import org.mobicents.charging.server.data.UserSessionInfo;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.SbbLocalObjectExt;

/**
 * Diameter Charging Server Root SBB.
 * 
 * @author ammendonca
 * @author baranowb
 * @author rsaranathan
 */
public abstract class DiameterChargingServerSbb extends BaseSbb implements Sbb, DiameterChargingServer {

	private static final long DEFAULT_VALIDITY_TIME = 86400;
	private static final TimerOptions DEFAULT_TIMER_OPTIONS = new TimerOptions(0, TimerPreserveMissed.ALL);

	private boolean performRating = false; // true = centralized, false = decentralized (ie, has been done by CTF (eg SIP AS))
	private boolean generateCDR = false;

	private static TimerOptions createDefaultTimerOptions() {
		TimerOptions timerOptions = new TimerOptions();
		timerOptions.setPreserveMissed(TimerPreserveMissed.ALL);
		return timerOptions;
	}

	private SbbContextExt sbbContextExt; // This SBB's SbbContext

	private Tracer tracer;
	private TimerFacility timerFacility;

	private RoAvpFactory avpFactory;
	//private RoActivityContextInterfaceFactory roAcif;
	private RoProvider roProvider;

	private AccountBalanceManagement accountBalanceManagement = null;
	private RatingEngineClient ratingEngineManagement = null;
	private CDRGenerator cdrGenerator = null;

	private String sidString = "SID<Unknown/?#?>";

	private static HashMap<String, String> abmfAVPs = new HashMap<String, String>();

	// ---------------------------- SLEE Callbacks ----------------------------

	public void setSbbContext(SbbContext context) {
		this.sbbContextExt = (SbbContextExt) context;
		this.tracer = sbbContextExt.getTracer("CS-Core");
		this.timerFacility = this.sbbContextExt.getTimerFacility();

		ResourceAdaptorTypeID raTypeID = new ResourceAdaptorTypeID("Diameter Ro", "java.net", "0.8.1");
		this.roProvider = (RoProvider) sbbContextExt.getResourceAdaptorInterface(raTypeID, "DiameterRo");
		//this.roAcif = (RoActivityContextInterfaceFactory) sbbContextExt.getActivityContextInterfaceFactory(raTypeID);

		this.avpFactory = this.roProvider.getRoAvpFactory();
	}

	public void unsetSbbContext() {
		this.sbbContextExt = null;
		this.tracer = null;
	}

	/**
	 * Convenience method to retrieve the SbbContext object stored in
	 * setSbbContext.
	 * 
	 * @return this SBB's SbbContext object
	 */
	protected SbbContext getSbbContext() {
		return sbbContextExt;
	}

	// ---------------------------- Child Relation ----------------------------
	public abstract ChildRelation getAccountBalanceManagementChildRelation();

	public abstract ChildRelation getDatasourceChildRelation();

	public abstract ChildRelation getRatingEngineChildRelation();

	public abstract ChildRelation getCDRGeneratorChildRelation();

	// --------------------------------- IES ----------------------------------
	public InitialEventSelector onCreditControlRequestInitialEventSelect(InitialEventSelector ies) {
		RoCreditControlRequest event = (RoCreditControlRequest) ies.getEvent();

		String sid = event.getSessionId();
		ies.setCustomName(sid);
		// ammendonca: only INITIAL are initial events
		boolean isInitial = (event.getCcRequestType() == CcRequestType.INITIAL_REQUEST || event.getCcRequestType() == CcRequestType.EVENT_REQUEST);
		if (tracer.isFineEnabled()) {
			tracer.fine("[--] SID<" + limitString(sid, 9, 9, "..") + "> Received CCR is " + (isInitial ? "" : "non-") + "initial.");
		}
		ies.setInitialEvent(isInitial);

		return ies;
	}

	// ---------------------------- Helper Methods ----------------------------

	private static final String DATASOURCE_CHILD_NAME = "DATASOURCE";
	protected DataSource getDatasource() throws IllegalArgumentException, NullPointerException, SLEEException, CreateException {
		ChildRelationExt cre = (ChildRelationExt) getDatasourceChildRelation();
		SbbLocalObjectExt sbbLocalObject = cre.get(DATASOURCE_CHILD_NAME);
		if (sbbLocalObject == null) {
			sbbLocalObject = cre.create(DATASOURCE_CHILD_NAME);
		}

		return (DataSource) sbbLocalObject;
	}

	private static final String ABMF_CHILD_NAME = "ACC_MANAGER";
	protected AccountBalanceManagement getAccountManager() throws IllegalArgumentException, NullPointerException, SLEEException, CreateException {
		ChildRelationExt cre = (ChildRelationExt) getAccountBalanceManagementChildRelation();
		SbbLocalObjectExt sbbLocalObject = cre.get(ABMF_CHILD_NAME);
		if (sbbLocalObject == null) {
			sbbLocalObject = cre.create(ABMF_CHILD_NAME);
		}

		return (AccountBalanceManagement) sbbLocalObject;
	}

	private static final String RATING_CHILD_NAME = "RE_MANAGER";
	protected RatingEngineClient getRatingEngineManager() throws IllegalArgumentException, NullPointerException, SLEEException, CreateException {
		ChildRelationExt cre = (ChildRelationExt) getRatingEngineChildRelation();
		SbbLocalObjectExt sbbLocalObject = cre.get(RATING_CHILD_NAME);
		if (sbbLocalObject == null) {
			sbbLocalObject = cre.create(RATING_CHILD_NAME);
		}

		return (RatingEngineClient) sbbLocalObject;
	}

	private static final String CDRGEN_CHILD_NAME = "CDR_GENERATOR";
	protected CDRGenerator getCDRGenerator() throws IllegalArgumentException, NullPointerException, SLEEException, CreateException {
		ChildRelationExt cre = (ChildRelationExt) getCDRGeneratorChildRelation();
		SbbLocalObjectExt sbbLocalObject = cre.get(CDRGEN_CHILD_NAME);
		if (sbbLocalObject == null) {
			sbbLocalObject = cre.create(CDRGEN_CHILD_NAME);
		}

		return (CDRGenerator) sbbLocalObject;
	}

	/**
	 * @param errorCodeType
	 * @return
	 */
	protected long getResultCode(ErrorCodeType errorCodeType) {
		switch (errorCodeType) {
		// actually return codes are not 100% ok here.
		case InvalidUser:
			return CreditControlResultCode.DIAMETER_USER_UNKNOWN;
		case BadRoamingCountry:
			return CreditControlResultCode.DIAMETER_END_USER_SERVICE_DENIED;
		case NoServiceForUser:
			return CreditControlResultCode.DIAMETER_END_USER_SERVICE_DENIED;
		case NotEnoughBalance:
			return CreditControlResultCode.DIAMETER_CREDIT_LIMIT_REACHED;
		case InvalidContent:
		case MalformedRequest:
		case AccountingConnectionErr:
		default:
			// TODO: Improve with more specific codes
			return DiameterResultCode.DIAMETER_UNABLE_TO_COMPLY;
		}
	}

	// ---------------------------- Event Handlers ----------------------------

	public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci) {
		if (tracer.isInfoEnabled()) {
			tracer.info("==============================================================================");
			tracer.info("==                 Mobicents Charging Server v1.0 [STARTED]                 ==");
			tracer.info("==                                  - . -                                   ==");
			tracer.info("==              Thank you for running Mobicents Community code              ==");
			tracer.info("==   For Commercial Grade Support, please request a TelScale Subscription   ==");
			tracer.info("==                         http://www.telestax.com/                         ==");
			tracer.info("==============================================================================");
		}

		DataSource ds;
		try {
			ds = getDatasource();
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] Got DataSource Child SBB Local Interface [" + ds + "]");
			}
			ds.init();
		}
		catch (Exception e) {
			tracer.severe("[xx] Unable to fetch Datasource Child SBB .");
			return;
		}

		AccountBalanceManagement am;
		try {
			am = getAccountManager();
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] Got Account Balance Management Child SBB Local Interface [" + am + "]");
			}
		}
		catch (Exception e) {
			tracer.severe("[xx] Unable to fetch Account and Balance Management Child SBB .");
			return;
		}

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			boolean loadUsersFromCSV = (Boolean) loadEnvEntry(ctx, "loadUsersFromCSV", false);
			performRating = (Boolean) loadEnvEntry(ctx, "performRating", false);
			generateCDR = (Boolean) loadEnvEntry(ctx, "generateCDR", false);
			String abmfAVPsProp = (String) loadEnvEntry(ctx, "ABMF_AVPs", "");


			try {
				String[] avps = abmfAVPsProp.trim().split(",");
				for (String avp : avps) {
					String[] codeName = avp.trim().split("=");
					if (tracer.isInfoEnabled()) {
						tracer.info("[><] Mapping AVP with Code " + codeName[0] + " as '" + codeName[1] + "' on received CCRs for ABMF Data.");
					}
					abmfAVPs.put(codeName[0], codeName[1]);
				}
			}
			catch (Exception e) {
				tracer.warning("[!!] Error reading ABMF Data AVPs. Format should be: code=name,code2=name2,... No custom data will be passed.");
			}

			if (loadUsersFromCSV) {
				try {

					Properties props = new Properties();
					props.load(this.getClass().getClassLoader().getResourceAsStream("users.properties"));
					for (Object key : props.keySet()) {
						String msisdn = (String) key;
						// am.addUser(imsi, Long.valueOf(props.getProperty(imsi)));
						// TODO: remove the properties to database mapping later on. useful for now
						ds.updateUser(msisdn, Long.valueOf(props.getProperty(msisdn)));
					}
					if (tracer.isInfoEnabled()) {
						tracer.info("[--] Loaded users from properties file.");
					}
				}
				catch (Exception e) {
					tracer.warning("[!!] Unable to load users from properties file. Allowing everything!");
					am.setBypass(true);
				}
				finally {
					if (tracer.isFineEnabled()) {
						tracer.fine("[--] Dumping users state...");
						am.dump("%");
					}
				}
			}
		}
		catch(Exception e) {
			tracer.warning("[!!] Unable to retrieve InitialContext. The env-entry properties were not loaded and defaults will be used.");
		}
	}

	/**
	 * Helper method to load env-entry. In case of exception returns the default value
	 *
	 * @param ctx the context to lookup at
	 * @param name the name of the env-entry
	 * @param deFault the value to return in case the env-entry is not present
	 * @return the value of the env-entry, or default if not present
	 */
	private Object loadEnvEntry(Context ctx, String name, Object deFault) {
		try {
			return ctx.lookup(name);
		}
		catch (Exception e) {
			tracer.warning("Unable to read '" + name + "' env entry. Defaulting to " + deFault + ".");
			return deFault;
		}
	}

	public void onCreditControlRequest(RoCreditControlRequest ccr, ActivityContextInterface aci) {
		String serviceContextId = "Some-Service-Context-Id";

		String sessionId = ccr.getSessionId();

		UserSessionInfo sessionInfo = getSessionInfo();
		if (sessionInfo == null) {
			sessionInfo = new UserSessionInfo();
			sessionInfo.setSessionStartTime(System.currentTimeMillis());
		}
		sessionInfo.setCcr(ccr);
		sessionInfo.setSessionId(sessionId);
		setSessionInfo(sessionInfo);

		String reqType = ccr.getCcRequestType().toString();
		long reqNumber = ccr.getCcRequestNumber();
		sidString = "SID<" + limitString(sessionId, 9, 9, "..") + "/" + reqType.substring(0, 3) + "#" + reqNumber + ">";

		if (tracer.isInfoEnabled()) {
			tracer.info("[<<] " + sidString + " Received Credit-Control-Request [" + reqType + "]");
			if (tracer.isFineEnabled()) {
				tracer.fine(ccr.toString());
			}
		}

		// Some common ops. may be moved to proper places to avoid unnecessary ops
		RoServerSessionActivity ccServerActivity = (RoServerSessionActivity) aci.getActivity();

		SubscriptionIdType endUserType = null; 
		String endUserId = null;

		// Get the Subscription-Id and it's Type .. for now we only care for first, still we log all
		SubscriptionIdAvp[] subscriptionIds = ccr.getSubscriptionIds();

		RoCreditControlAnswer cca = null;
		if (subscriptionIds != null && subscriptionIds.length > 0) {
			endUserType = subscriptionIds[0].getSubscriptionIdType();
			endUserId = subscriptionIds[0].getSubscriptionIdData();
			if (tracer.isFineEnabled()) {
				String subsIdsStr = "";
				for (SubscriptionIdAvp subscriptionId : subscriptionIds) {
					subsIdsStr += subscriptionId.getSubscriptionIdType() + "=" + subscriptionId.getSubscriptionIdData() + " ";
				}
				tracer.fine("[--] " + sidString + " Received CCR has Subcription-Id(s): " + subsIdsStr.substring(0, subsIdsStr.length()-1));
			}
		}
		else {
			tracer.severe("[xx] " + sidString + " Subscription-Id AVP missing in CCR. Rejecting CCR.");
			cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_MISSING_AVP);
			sendCCA(cca, aci, true);
			return;
		}

		if (endUserId == null) {
			tracer.severe("[xx] " + sidString + " Subscription-Id AVP is present but could not read it's data. Rejecting CCR.");
			cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_MISSING_AVP);
			sendCCA(cca, aci, true);
			return;
		}

		// Retrieve child SBBs
		try {
			accountBalanceManagement = getAccountManager();
			ratingEngineManagement = getRatingEngineManager();
			cdrGenerator = getCDRGenerator();
		}
		catch (Exception e) {
			// TODO: By configuration it should be possible to proceed
			tracer.severe("[xx] " + sidString + " Unable to retrieve Account & Balance Management or Rating Child SBB. Unable to continue.", e);
			cca = createCCA(ccServerActivity, ccr, new ArrayList<CreditControlInfo>(), DiameterResultCode.DIAMETER_UNABLE_TO_COMPLY);
			sendCCA(cca, aci, true);
		}

		switch (ccr.getCcRequestType()) {
		// INITIAL_REQUEST 1
		case INITIAL_REQUEST:
			// ... intentionally did not break;
			// UPDATE_REQUEST 2
		case UPDATE_REQUEST:
			// FIXME: We may need to set timeout timer.. but not like this.
			// timerFacility.setTimer(aci, null, System.currentTimeMillis() + 15000, DEFAULT_TIMER_OPTIONS);

			try {
				// retrieve service information from AVPs
				serviceContextId = ccr.getServiceContextId();
				if (serviceContextId == null) {
					tracer.severe("[xx] " + sidString + " Service-Context-Id AVP missing in CCR. Rejecting CCR.");
					// TODO: include missing avp - its a "SHOULD"
					cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_MISSING_AVP);
					sendCCA(cca, aci, true);
				}
				else {
					if (serviceContextId.equals("")) {
						tracer.severe("[xx] " + sidString + " Service-Context-Id AVP is empty in CCR. Rejecting CCR.");
						cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_INVALID_AVP_VALUE);
						sendCCA(cca, aci, true);
					}
				}

				// TODO: For Ro, support Service-Information AVP

				List<CreditControlInfo> reservations = new ArrayList<CreditControlInfo>();
				long resultCode = DiameterResultCode.DIAMETER_SUCCESS;

				MultipleServicesCreditControlAvp[] multipleServicesCreditControlAvps = ccr.getMultipleServicesCreditControls();
				if (multipleServicesCreditControlAvps != null && tracer.isFineEnabled()) {
					tracer.fine("[--] " + sidString + " Received CCR has Multiple-Services-Credit-Control AVP with length = " + multipleServicesCreditControlAvps.length);
				}

				// If there's no MSCC AVP, we'll create one, just to go inside the for and have it processed..
				if(multipleServicesCreditControlAvps.length == 0) {
					MultipleServicesCreditControlAvp fakeMSCC = avpFactory.createMultipleServicesCreditControl();
					fakeMSCC.setServiceIdentifier(0);
					RequestedServiceUnitAvp rsu = avpFactory.createRequestedServiceUnit();
					rsu.setCreditControlTotalOctets(0);
					fakeMSCC.setRequestedServiceUnit(rsu);
					multipleServicesCreditControlAvps = new MultipleServicesCreditControlAvp[]{fakeMSCC};
				}

				// RFC4006 / 8.16.  Multiple-Services-Credit-Control AVP
				// Note that each instance of this AVP carries units related to one or more services or related to a
				// single rating group.
				for (MultipleServicesCreditControlAvp mscc : multipleServicesCreditControlAvps) {

					// The Service-Identifier and the Rating-Group AVPs are used to associate the granted units to a
					// given service or rating group.  If both the Service-Identifier and the Rating-Group AVPs are
					// included, the target of the service units is always the service(s) indicated by the value of the
					// Service-Identifier AVP(s).  If only the Rating-Group-Id AVP is present, the Multiple-Services-
					// -Credit-Control AVP relates to all the services that belong to the specified rating group.

					long ratingGroup = mscc.getRatingGroup();
					long[] serviceIds = mscc.getServiceIdentifiers();

					// The Requested-Service-Unit AVP MAY contain the amount of requested service units [...]. It MUST
					// be present in the initial interrogation and within the intermediate interrogations in which new
					// quota is requested.  If the credit-control client does not include the Requested-Service-Unit AVP
					// in a request command, because for instance, it has determined that the end-user terminated the
					// service, the server MUST debit the used amount from the user's account but MUST NOT return a new
					// quota in the corresponding answer.

					RequestedServiceUnitAvp rsu = mscc.getRequestedServiceUnit();
					ArrayList<CreditControlUnit> ccUnits = getRequestedUnits(ccr, rsu, serviceIds);

					// if its UPDATE, lets first update data
					if (ccr.getCcRequestType() == CcRequestType.UPDATE_REQUEST) {
						// update used units for each CC-Type.
						UsedServiceUnitAvp[] usedUnitsAvps = mscc.getUsedServiceUnits();

						sessionInfo = getSessionInfo();
						CreditControlInfo reservedInfo = sessionInfo.getReservations().get(sessionInfo.getReservations().size() - 1);

						ArrayList<CreditControlUnit> usedCCUnits = collectUsedUnits(usedUnitsAvps, reservedInfo.getCcUnits());

						// Merge Requested with Used/Reserved CC Units into a single CCUnits
						ccUnits.addAll(usedCCUnits);

						// Call ABMF with this Credit Control Info
						CreditControlInfo ccInfo = buildCCInfo(ccr, endUserId, endUserType, ccUnits);
						accountBalanceManagement.updateRequest(ccInfo);
					}
					else {
						// Initial Request

						// Call ABMF with this Credit Control Info
						CreditControlInfo ccInfo = buildCCInfo(ccr, endUserId, endUserType, ccUnits);
						accountBalanceManagement.initialRequest(ccInfo);
					}

					// Store Credit Control Info in CMP
					sessionInfo = getSessionInfo();
					sessionInfo.setCcr(ccr);
					sessionInfo.setServiceIds(serviceIds);
					sessionInfo.setEndUserId(endUserId);
					sessionInfo.setEndUserType(endUserType);
					setSessionInfo(sessionInfo);

					return; // we'll continue @ resumeOnCreditControlRequest(..)
				}

				if (reservations.size() > 0) {
					cca = createCCA(ccServerActivity, ccr, reservations, resultCode);
				}
				else {
					cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_MISSING_AVP);
				}
				sendCCA(cca, aci, false);
			}
			catch (Exception e) {
				tracer.severe("[xx] " + sidString + " Failure processing Credit-Control-Request [" + (ccr.getCcRequestType() == CcRequestType.INITIAL_REQUEST ? "INITIAL" : "UPDATE") + "]", e);
			}
			break;
			// TERMINATION_REQUEST 3
		case TERMINATION_REQUEST:
			try {
				if (tracer.isInfoEnabled()) {
					tracer.info("[>>] " + sidString + " '" + endUserId + "' requested service termination for '" + serviceContextId + "'.");
				}

				for (MultipleServicesCreditControlAvp mscc : ccr.getMultipleServicesCreditControls()) {

					UsedServiceUnitAvp[] usedUnitsAvps = mscc.getUsedServiceUnits();

					sessionInfo = getSessionInfo();
					CreditControlInfo reservedInfo = sessionInfo.getReservations().get(sessionInfo.getReservations().size() - 1);

					ArrayList<CreditControlUnit> ccUnits = collectUsedUnits(usedUnitsAvps, reservedInfo.getCcUnits());

					// Call ABMF with this Credit Control Info
					CreditControlInfo ccInfo = buildCCInfo(ccr, endUserId, endUserType, ccUnits);
					accountBalanceManagement.terminateRequest(ccInfo);

					// No need to Store Credit Control Info in CMP. SLEE Container automatically takes care of garbage collection.
					// sessionInfo = getSessionInfo();
					// sessionInfo.getReservations().add(ccInfo);
					// setSessionInfo(sessionInfo);

					return; // we'll continue @ resumeOnCreditControlRequest(..)
				}

				// 8.7.  Cost-Information AVP
				//
				// The Cost-Information AVP (AVP Code 423) is of type Grouped, and it is
				// used to return the cost information of a service, which the credit-
				// control client can transfer transparently to the end user.  The
				// included Unit-Value AVP contains the cost estimate (always type of
				// money) of the service, in the case of price enquiry, or the
				// accumulated cost estimation, in the case of credit-control session.
				//
				// The Currency-Code specifies in which currency the cost was given.
				// The Cost-Unit specifies the unit when the service cost is a cost per
				// unit (e.g., cost for the service is $1 per minute).
				//
				// When the Requested-Action AVP with value PRICE_ENQUIRY is included in
				// the Credit-Control-Request command, the Cost-Information AVP sent in
				// the succeeding Credit-Control-Answer command contains the cost
				// estimation of the requested service, without any reservation being
				// made.
				//
				// The Cost-Information AVP included in the Credit-Control-Answer
				// command with the CC-Request-Type set to UPDATE_REQUEST contains the
				// accumulated cost estimation for the session, without taking any
				// credit reservation into account.
				//
				// The Cost-Information AVP included in the Credit-Control-Answer
				// command with the CC-Request-Type set to EVENT_REQUEST or
				// TERMINATION_REQUEST contains the estimated total cost for the
				// requested service.
				//
				// It is defined as follows (per the grouped-avp-def of
				// RFC 3588 [DIAMBASE]):
				//
				//           Cost-Information ::= < AVP Header: 423 >
				//                                { Unit-Value }
				//                                { Currency-Code }
				//                                [ Cost-Unit ]

				// 7.2.133 Remaining-Balance AVP
				//
				// The Remaining-Balance AVP (AVPcode 2021) is of type Grouped and
				// provides information about the remaining account balance of the
				// subscriber.
				//
				// It has the following ABNF grammar:
				//      Remaining-Balance :: =  < AVP Header: 2021 >
				//                              { Unit-Value }
				//                              { Currency-Code }

				// We use no money notion ... maybe later.
				// AvpSet costInformation = ccaAvps.addGroupedAvp(423);

				// Answer with DIAMETER_SUCCESS, since "4) The default action for failed operations should be to terminate the data session"
				// its terminated, we cant do much here...
				cca = createCCA(ccServerActivity, ccr, null, DiameterResultCode.DIAMETER_SUCCESS);
				sendCCA(cca, aci, true);
			}
			catch (Exception e) {
				tracer.severe("[xx] " + sidString + " Failure processing Credit-Control-Request [TERMINATION]", e);
			}
			break;
			// EVENT_REQUEST 4
		case EVENT_REQUEST:
			try {
				RequestedActionType reqAction = ccr.getRequestedAction();
				if (tracer.isInfoEnabled()) {
					tracer.info("[<<] " + sidString + " Received Credit-Control-Request [EVENT] with Requested-Action [" + reqAction + "]");

					if (tracer.isFineEnabled()) {
						tracer.fine(ccr.toString());
					}
				}

				if (reqAction == null) {
					tracer.severe("[xx] " + sidString + " Unable to retrieve Requested-Action AVP. Replying with MISSING_AVP.");
					createCCA(ccServerActivity, ccr, new ArrayList<CreditControlInfo>(), DiameterResultCode.DIAMETER_MISSING_AVP);
					sendCCA(cca, aci, true);
				}
				else if (reqAction == RequestedActionType.DIRECT_DEBITING) {
					for (MultipleServicesCreditControlAvp mscc : ccr.getMultipleServicesCreditControls()) {
						RequestedServiceUnitAvp rsu = mscc.getRequestedServiceUnit();

						long[] serviceIds = mscc.getServiceIdentifiers();

						ArrayList<CreditControlUnit> ccUnits = getRequestedUnits(ccr, rsu, serviceIds);

						// Call ABMF with this Credit Control Info
						CreditControlInfo ccInfo = buildCCInfo(ccr, endUserId, endUserType, ccUnits);
						accountBalanceManagement.eventRequest(ccInfo);

						// Store Credit Control Info in CMP
						sessionInfo = getSessionInfo();
						sessionInfo.setCcr(ccr);
						sessionInfo.setEndUserId(endUserId);
						//sessionInfo.getReservations().add(ccInfo);
						setSessionInfo(sessionInfo);

						if (tracer.isInfoEnabled()) {
							tracer.info(sessionInfo.toString());
						}

						return; // we'll continue @ resumeOnCreditControlRequest(..)
					}
				}
				else {
					tracer.severe("[xx] " + sidString + " Unsupported Requested-Action AVP (" + reqAction + "). Replying with DIAMETER_UNABLE_TO_COMPLY.");
					createCCA(ccServerActivity, ccr, new ArrayList<CreditControlInfo>(), DiameterResultCode.DIAMETER_UNABLE_TO_COMPLY);
					sendCCA(cca, aci, true);
				}
			}
			catch (Exception e) {
				tracer.severe("[xx] " + sidString + " Failure processing Credit-Control-Request [EVENT]", e);
			}
			break;
		default:
			tracer.warning("[xx] " + sidString + " Unknown request type found!");
			break;
		}
	}

	private CreditControlInfo buildCCInfo(RoCreditControlRequest ccr, String endUserId, SubscriptionIdType endUserType, ArrayList<CreditControlUnit> ccUnits) {
		// Build Credit Control Info Request to ABMF
		CreditControlInfo ccInfo = new CreditControlInfo();
		ccInfo.setEventTimestamp(System.currentTimeMillis());
		CcRequestType type = ccr.getCcRequestType();
		ccInfo.setEventType(type.toString());
		if (type == CcRequestType.EVENT_REQUEST) {
			ccInfo.setRequestedAction(ccr.getRequestedAction());
		}
		ccInfo.setRequestNumber((int) ccr.getCcRequestNumber());
		ccInfo.setSessionId(ccr.getSessionId());
		ccInfo.setSubscriptionId(endUserId);
		ccInfo.setSubscriptionIdType(endUserType);
		ccInfo.setCcUnits(ccUnits);

		// Iterate CCR to capture needed AVPs
		for (DiameterAvp avp : ccr.getAvps()) {
			fetchDataFromAvp(avp, ccInfo);
		}

		return ccInfo;
	}

	public void onTimerEvent(TimerEvent timer, ActivityContextInterface aci) {
		// detach from this activity, we don't want to handle any other event on it
		aci.detach(this.sbbContextExt.getSbbLocalObject());
		if (tracer.isInfoEnabled()) {
			tracer.info("[--] " + sidString + " Forcing Activity Termination '" + aci.getActivity() + "' due to timeout expire.");
		}
		// TODO: allow for different options, such as sending a RAR request.
		((RoServerSessionActivity) aci.getActivity()).endActivity();
	}

	/**
	 * @param ccServerActivity
	 * @param request
	 * @param reservations
	 * @param resultCode
	 * @return
	 */
	private RoCreditControlAnswer createCCA(RoServerSessionActivity ccServerActivity, RoCreditControlRequest request, List<CreditControlInfo> reservations, long resultCode) {
		RoCreditControlAnswer answer = ccServerActivity.createRoCreditControlAnswer();

		// <Credit-Control-Answer> ::= < Diameter Header: 272, PXY >
		//  < Session-Id >
		//  { Result-Code }
		answer.setResultCode(resultCode);
		//  { Origin-Host }
		//  { Origin-Realm }
		//  { Auth-Application-Id }

		//  { CC-Request-Type }
		// Using the same as the one present in request
		// answer.setCcRequestType(request.getCcRequestType()); // Added from Request, no need to add manually.

		//  { CC-Request-Number }
		// Using the same as the one present in request
		// answer.setCcRequestNumber(request.getCcRequestNumber()); // Added from Request, no need to add manually.

		//  [ User-Name ]
		//  [ CC-Session-Failover ]
		//  [ CC-Sub-Session-Id ]
		//  [ Acct-Multi-Session-Id ]
		//  [ Origin-State-Id ]
		//  [ Event-Timestamp ]

		//  [ Granted-Service-Unit ]
		//
		// 8.17.  Granted-Service-Unit AVP
		//
		// Granted-Service-Unit AVP (AVP Code 431) is of type Grouped and
		// contains the amount of units that the Diameter credit-control client
		// can provide to the end user until the service must be released or the
		// new Credit-Control-Request must be sent.  A client is not required to
		// implement all the unit types, and it must treat unknown or
		// unsupported unit types in the answer message as an incorrect CCA
		// answer.  In this case, the client MUST terminate the credit-control
		// session and indicate in the Termination-Cause AVP reason
		// DIAMETER_BAD_ANSWER.
		//
		// The Granted-Service-Unit AVP is defined as follows (per the grouped-
		// avp-def of RFC 3588 [DIAMBASE]):
		//
		// Granted-Service-Unit ::= < AVP Header: 431 >
		//                          [ Tariff-Time-Change ]
		//                          [ CC-Time ]
		//                          [ CC-Money ]
		//                          [ CC-Total-Octets ]
		//                          [ CC-Input-Octets ]
		//                          [ CC-Output-Octets ]
		//                          [ CC-Service-Specific-Units ]
		//                         *[ AVP ]
		if (reservations != null && reservations.size() > 0) {
			MultipleServicesCreditControlAvp[] reqMSCCs = request.getMultipleServicesCreditControls();
			List<MultipleServicesCreditControlAvp> ansMSCCs = new ArrayList<MultipleServicesCreditControlAvp>();
			for (int index = 0; index < reqMSCCs.length; index++) {
				MultipleServicesCreditControlAvp reqMSCC = reqMSCCs[index];
				MultipleServicesCreditControlAvp ansMscc = avpFactory.createMultipleServicesCreditControl();
				ansMscc.setRatingGroup(reqMSCC.getRatingGroup());
				ansMscc.setServiceIdentifiers(reqMSCC.getServiceIdentifiers());
				// FIXME: Check how to handle this in case of MSCC if anything different is needed
				CreditControlInfo ccInfo = reservations.get(reservations.size()-1);
				if (ccInfo.isSuccessful()) {
					GrantedServiceUnitAvp gsu = avpFactory.createGrantedServiceUnit();
					ArrayList<CreditControlUnit> ccUnits = ccInfo.getCcUnits();
					for (int i = 0; i < ccUnits.size(); i++) {
						CreditControlUnit ccUnit = ccUnits.get(i);
						if (ccUnit.getUnitType() == CcUnitType.INPUT_OCTETS) {
							gsu.setCreditControlInputOctets(ccUnit.getReservedUnits());
						}
						// TODO: Add CC-Money support if not 3GPP ?
						if (ccUnit.getUnitType() == CcUnitType.OUTPUT_OCTETS) {
							gsu.setCreditControlOutputOctets(ccUnit.getReservedUnits());
						}
						if (ccUnit.getUnitType() == CcUnitType.SERVICE_SPECIFIC_UNITS) {
							gsu.setCreditControlServiceSpecificUnits(ccUnit.getReservedUnits());
						}
						if (ccUnit.getUnitType() == CcUnitType.TIME) {
							gsu.setCreditControlTime(ccUnit.getReservedUnits());
						}
						if (ccUnit.getUnitType() == CcUnitType.TOTAL_OCTETS) {
							gsu.setCreditControlTotalOctets(ccUnit.getReservedUnits());
						}
					}
					ansMscc.setGrantedServiceUnit(gsu);
					ansMscc.setResultCode(DiameterResultCode.DIAMETER_SUCCESS);

					// TODO: Have Final-Unit-Indication when needed...
					// If we are terminating gracefully we MAY include the Final-Unit-Indication
					if (answer.getCcRequestType() == CcRequestType.TERMINATION_REQUEST) {
						FinalUnitIndicationAvp fuiAvp = avpFactory.createFinalUnitIndication();
						fuiAvp.setFinalUnitAction(FinalUnitActionType.TERMINATE);
						ansMscc.setFinalUnitIndication(fuiAvp);
					}
				}
				else {
					// In case it's not successful we want to have Final-Unit-Indication
					FinalUnitIndicationAvp fuiAvp = avpFactory.createFinalUnitIndication();
					fuiAvp.setFinalUnitAction(FinalUnitActionType.TERMINATE);
					ansMscc.setFinalUnitIndication(fuiAvp);

					ansMscc.setResultCode(resultCode);
				}
				ansMSCCs.add(ansMscc);
				ansMscc.setValidityTime(DEFAULT_VALIDITY_TIME);
			}
			answer.setMultipleServicesCreditControls(ansMSCCs.toArray(new MultipleServicesCreditControlAvp[ansMSCCs.size()]));
		}

		// *[ Multiple-Services-Credit-Control ]
		//  [ Cost-Information]
		//  [ Final-Unit-Indication ]
		//  [ Check-Balance-Result ]
		//  [ Credit-Control-Failure-Handling ]
		//  [ Direct-Debiting-Failure-Handling ]
		//  [ Validity-Time]
		//Ro does not use message level VT
		// *[ Redirect-Host]
		//  [ Redirect-Host-Usage ]
		//  [ Redirect-Max-Cache-Time ]
		// *[ Proxy-Info ]
		// *[ Route-Record ]
		// *[ Failed-AVP ]
		// *[ AVP ]

		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] " + sidString + " Created Credit-Control-Answer with Result-Code = " + answer.getResultCode() + ".");
			if (tracer.isFineEnabled()) {
				tracer.fine(answer.toString());
			}
		}

		return answer;
	}

	/**
	 * Sends the Credit-Control-Answer through the ACI and detaches if set to.
	 * @param cca the Credit-Control-Answer to send
	 * @param aci the ACI where to send from
	 * @param detach boolean indicating whether to detach or not
	 * @return true if it succeeds sending, false otherwise
	 */
	private boolean sendCCA(RoCreditControlAnswer cca, ActivityContextInterface aci, boolean detach) {
		// Start by cancelling any existing Timer
		TimerID timerID = getTimerID();
		if (timerID != null) {
			timerFacility.cancelTimer(timerID);
			if(tracer.isFineEnabled()) {
				tracer.info("[><] " + sidString + " Cancelling existing timer " + timerID);
			}
		}
		// Set a new one, unless we are leaving...
		if (!detach) {
			timerID = timerFacility.setTimer(aci, null, System.currentTimeMillis() + DEFAULT_VALIDITY_TIME*1000, DEFAULT_TIMER_OPTIONS);
			setTimerID(timerID);
			if(tracer.isFineEnabled()) {
				tracer.fine("[><] " + sidString + " Setting new timer " + timerID + " for " + System.currentTimeMillis() + DEFAULT_VALIDITY_TIME*1000);
			}
		}
		try {
			RoServerSessionActivity ccServerActivity = (RoServerSessionActivity) aci.getActivity();
			ccServerActivity.sendRoCreditControlAnswer(cca);
			if (detach) {
				if (tracer.isFineEnabled()) {
					tracer.fine("[><] " + sidString + " Detaching from ACI.");
				}
				aci.detach(this.getSbbContext().getSbbLocalObject());
			}
			return true;
		}
		catch (IOException e) {
			tracer.severe("[xx] " + sidString + " Error while trying to send Credit-Control-Answer.", e);
			return false;
		}
	}

	//private String storedEndUserId;
	//private long storedRequestedUnits;
	//private long[] storedServiceIds;
	//private ArrayList<UnitReservation> storedReservations = new ArrayList<UnitReservation>();

	@Override
	public void resumeOnCreditControlRequest(CreditControlInfo ccInfo) {
		UserSessionInfo sessionInfo = getSessionInfo();
		RoCreditControlRequest storedCCR = sessionInfo.getCcr();
		if (tracer.isInfoEnabled()) {
			tracer.info("[<<] " + sidString + " Resuming Handling of Credit-Control-Request [" + storedCCR.getCcRequestType().toString() + "]");
		}
		if (tracer.isFineEnabled()) {
			tracer.fine("[<<] \" + sidString + \" " + ccInfo);
		}
		sessionInfo.getReservations().add(ccInfo);
		setSessionInfo(sessionInfo);
		long resultCode = DiameterResultCode.DIAMETER_SUCCESS;
		if (ccInfo.isSuccessful()) {
			if (tracer.isInfoEnabled()) {
				tracer.info("[>>] " + sidString + " '" + sessionInfo.getEndUserId() + "' GRANTED for '" + Arrays.toString(sessionInfo.getServiceIds()) + "'.");
			}
		}
		else {
			if (tracer.isInfoEnabled()) {
				tracer.info("[>>] " + sidString + " '" + sessionInfo.getEndUserId() + "' DENIED for '" + Arrays.toString(sessionInfo.getServiceIds()) + "'.");
			}
			// If we can't determine error, say UNABLE_TO_COMPLY
			resultCode = ccInfo.getErrorCodeType() != null ? getResultCode(ccInfo.getErrorCodeType()) : DiameterResultCode.DIAMETER_UNABLE_TO_COMPLY;
		}

		try {
			ActivityContextInterface[] acis = this.sbbContextExt.getActivities();
			ActivityContextInterface aci = null;
			RoServerSessionActivity activity = null;
			for (ActivityContextInterface curAci : acis) {
				if (curAci.getActivity() instanceof RoServerSessionActivity) {
					aci = curAci;
					activity = (RoServerSessionActivity) curAci.getActivity();
					break;
				}
			}

			RoCreditControlAnswer cca = sessionInfo.getReservations().size() > 0 ? createCCA(activity, storedCCR, sessionInfo.getReservations(), resultCode) : createCCA(activity, storedCCR, null, DiameterResultCode.DIAMETER_MISSING_AVP);
			sendCCA(cca, aci, storedCCR.getCcRequestType() == CcRequestType.TERMINATION_REQUEST || storedCCR.getCcRequestType() == CcRequestType.EVENT_REQUEST);

			// Output the user session details.
			if (tracer.isInfoEnabled()) {
				tracer.info("[--] " + sidString + " CCA successfully sent.");
			}
			if (tracer.isFineEnabled()) {
				tracer.fine("[--] " + sidString + "Dumping session info...\n" + sessionInfo);
			}
		}
		catch (Exception e) {
			tracer.severe("[xx] " + sidString + " Unable to send Credit-Control-Answer.", e);
		}


		if (generateCDR && cdrGenerator != null && storedCCR.getCcRequestType() == CcRequestType.TERMINATION_REQUEST) {
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] " + sidString + " Generating CDR.");
			}

			try {
				cdrGenerator.writeCDR(sessionInfo);
			}
			catch (Exception e) {
				tracer.severe("[xx] " + sidString + " Unable to generate CDR", e);
			}
		}
	}

	@Override
	public void updateAccountDataResult(boolean success) {
		if (success) {
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] " + sidString + " Update User Account Data completed with success.");
			}
		}
		else {
			tracer.warning("[><] " + sidString + " Update User Account Data failed.");
		}
	}

	// --------- Call to decentralized rating engine ---------------------
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private double getRateForService(RoCreditControlRequest ccr, long serviceId, long unitTypeId, long requestedUnits) {

		// Let's make some variables available to be sent to the rating engine
		HashMap params = new HashMap();
		params.put("ChargingServerHost", ccr.getDestinationHost());
		params.put("SessionId", ccr.getSessionId());
		params.put("RequestType", ccr.getCcRequestType().toString());
		SubscriptionIdAvp[] subscriptionIds = ccr.getSubscriptionIds();
		boolean hasSubscriptionIds = (subscriptionIds != null && subscriptionIds.length > 0);
		params.put("SubscriptionIdType", hasSubscriptionIds ? subscriptionIds[0].getSubscriptionIdType().getValue() : -1);
		params.put("SubscriptionIdData", hasSubscriptionIds ? subscriptionIds[0].getSubscriptionIdData() : null);
		//params.put("UnitId", getUnitId((int)serviceId));
		params.put("UnitTypeId", unitTypeId);
		params.put("UnitValue", requestedUnits);
		params.put("ServiceId", serviceId);
		params.put("BeginTime", ccr.getEventTimestamp().getTime());
		params.put("ActualTime", System.currentTimeMillis());

		// TODO: Extract DestinationId AVP from the CCR if available.
		params.put("DestinationIdType", "?");
		params.put("DestinationIdData", "?");

		RatingInfo ratingInfo = ratingEngineManagement.getRateForService(params);

		// Retrieve the rating information [and optionally the unit type] from ratingInfo.

		int responseCode = ratingInfo.getResponseCode();
		double rate = 1.0;
		if (responseCode == 0) {
			// Rate obtained successfully from Rating Engine, let's use that.
			rate = ratingInfo.getRate();
		}
		else {
			// TODO: if rate was not found or error occurred while determining rate, what to do? Block traffic (certain types of traffic? for certain profiles? Allow for Free?)
			tracer.warning("[xx] " + sidString + " Unexpected response code '" + responseCode + "' received from Rating Engine.");
		}

		// allow traffic for free :(
		return rate;
	}

	@Override
	public void getRateForServiceResult(RatingInfo ratingInfo) {
		tracer.info("[><] " + sidString + " Got Rate for Service: " + ratingInfo);
	}

	// TODO: Ok, so let's not use this for now (the serviceid-units csv mapping file). Why?
	// According to 3GPP ref, it is not possible to have a decentralized rating engine and centralized unit
	// determination logic.
	//
	// 5.2.2	Charging Scenarios
	// In order to perform event charging via Ro, the scenarios between the involved entities UE-A, OCF and CTF need to
	// be defined. The charging flows shown in this subclause include scenarios with immediate event charging and event
	// charging with reservation. In particular, the following cases are shown:

	//	1	Immediate Event Charging
	//	a)	Decentralized Unit Determination and Centralized Rating
	//	b)	Centralized Unit Determination and Centralized Rating
	//	c)	Decentralized Unit Determination and Decentralized Rating

	//	2	Event charging with Reservation 
	//	a)	Decentralized Unit Determination and Centralized Rating
	//	b)	Centralized Unit Determination and Centralized Rating
	//	c)	Decentralized Unit Determination and Decentralized Rating

	//	3	Session charging with Reservation
	//	a)	Decentralized Unit Determination and Centralized Rating
	//	b)	Centralized Unit Determination and Centralized Rating
	//	c)	Decentralized Unit Determination and Decentralized Rating

	//	The combination of Centralized Unit Determination with Decentralized Rating is not possible.

	/**
	 * Convert IP4 address to String
	 * @param address byte array
	 * @return String
	 */
	private String byteArrayToStringIp(byte[] address) {
		if(address == null || address.length != 4) {
			return "0.0.0.0";
		}
		String stringIp = "";
		for (byte number:address) {
			stringIp+=(number & 0xFF) + ".";
		}
		return stringIp.substring(0, stringIp.length()-1);
	}

	/**
	 * Fetch data from AVP to be passed in CreditControlInfo, as configured in env entry.
	 * @param avp the AVP to look at
	 * @param ccInfo the CreditControlInfo object to store properties at
	 */
	private void fetchDataFromAvp(DiameterAvp avp, CreditControlInfo ccInfo) {
		fetchDataFromAvp(avp, ccInfo, 0);
	}

	/**
	 * Fetch data from AVP to be passed in CreditControlInfo, as configured in env entry.
	 * @param avp the AVP to look at
	 * @param ccInfo the CreditControlInfo object to store properties at
	 * @param depth the AVP depth, for recursive calls
	 */
	private void fetchDataFromAvp(DiameterAvp avp, CreditControlInfo ccInfo, int depth) {
		if (tracer.isFinerEnabled()) {
			tracer.finer("[><] " + sidString + " Scanning AVP at depth " + depth + " with code " + avp.getCode() + " and type " + avp.getType() + " ...");
		}
		if(avp.getType() == DiameterAvpType.GROUPED) {
			GroupedAvp gAvp = (GroupedAvp) avp;
			DiameterAvp[] subAvps = gAvp.getExtensionAvps();
			for(DiameterAvp subAvp : subAvps) {
				fetchDataFromAvp(subAvp, ccInfo, depth+1);
			}
		}
		else {
			String name = abmfAVPs.get(String.valueOf(avp.getCode()));
			if (name != null) {
				Object value = null;
				switch (avp.getType().getType())
				{
					case DiameterAvpType._ADDRESS:
					case DiameterAvpType._DIAMETER_IDENTITY:
					case DiameterAvpType._DIAMETER_URI:
					case DiameterAvpType._IP_FILTER_RULE:
					case DiameterAvpType._OCTET_STRING:
					case DiameterAvpType._QOS_FILTER_RULE:
						value = avp.octetStringValue();
						break;
					case DiameterAvpType._ENUMERATED:
					case DiameterAvpType._INTEGER_32:
						value = avp.intValue();
						break;
					case DiameterAvpType._FLOAT_32:
						value = avp.floatValue();
						break;
					case DiameterAvpType._FLOAT_64:
						value = avp.doubleValue();
						break;
					case DiameterAvpType._INTEGER_64:
						value = avp.longValue();
						break;
					case DiameterAvpType._TIME:
						value = avp.longValue();
						break;
					case DiameterAvpType._UNSIGNED_32:
						value = avp.longValue();
						break;
					case DiameterAvpType._UNSIGNED_64:
						value = avp.longValue();
						break;
					case DiameterAvpType._UTF8_STRING:
						value = avp.octetStringValue();
						break;
					default:
						value = avp.byteArrayValue();
						break;
				}
				if (tracer.isFineEnabled()) {
					tracer.fine("[><] " + sidString + " Storing AVP with code " + avp.getCode() + " as '" + name + "' with value '" + value.toString() + "'");
				}
				ccInfo.addServiceInfo(name, value.toString());
			}
		}
	}

	private ArrayList<CreditControlUnit> getRequestedUnits(RoCreditControlRequest ccr, RequestedServiceUnitAvp rsu, long[] serviceIds) {
		ArrayList<CreditControlUnit> ccRequestedUnits = new ArrayList<CreditControlUnit>();

		long requestedUnits = 0;
		for (int i = 0; i < CcUnitType.values().length; i++) {
			CcUnitType type = CcUnitType.fromInt(i);

			// MONEY is not supported by 3GPP. TODO: Add support for non 3GPP ?
			if (type == CcUnitType.MONEY) {
				continue;
			}

			String methodName = "getCreditControl" + toCamelCase(type.toString());
			try {
				Method m = rsu.getClass().getMethod(methodName, new Class[0]);
				requestedUnits = (Long) m.invoke(rsu, new Object[0]);

				if (tracer.isInfoEnabled() && requestedUnits != Long.MIN_VALUE) {
					tracer.info("[><] " + sidString + " Requested Units of type '" + type +  "' in CCR = " + requestedUnits);
				}

				if (requestedUnits >= 0) {
					CreditControlUnit ccUnit = new CreditControlUnit();
					ccUnit.setUnitType(type);
					if (performRating) {
						double rateForService = getRateForService(ccr, serviceIds[0], type.getValue(), requestedUnits);
						ccUnit.setRateForService(rateForService);
						// FIXME: This is not right. Rating should convert to monetary units...
						ccUnit.setRequestedAmount((long) Math.ceil(requestedUnits * rateForService));
					}
          else {
            ccUnit.setRequestedAmount(requestedUnits);
          }
					ccUnit.setRequestedUnits(requestedUnits);
					ccRequestedUnits.add(ccUnit);
				}
			}
			catch (Exception e) {
				tracer.severe("[xx] " + sidString + " Unable to retrieve/invoke '" + methodName + "' for extracting Requested Units of type " + type, e);
			}
		}

		return ccRequestedUnits;
	}

	private ArrayList<CreditControlUnit> collectUsedUnits(UsedServiceUnitAvp[] usuAvps, ArrayList<CreditControlUnit> reservedCCUnits) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] " + sidString + " Collecting " + usuAvps.length + " Used Units AVPs.");
		}
		ArrayList<CreditControlUnit> usedCCUnits = new ArrayList<CreditControlUnit>();
		for (UsedServiceUnitAvp usuAvp : usuAvps) {
			for (int n = 0; n < CcUnitType.values().length; n++) {
				CcUnitType type = CcUnitType.fromInt(n);

				// MONEY is not supported by 3GPP
				if (type == CcUnitType.MONEY) {
					continue;
				}

				String methodName = "getCreditControl" + toCamelCase(type.toString());
				try {
					Method m = usuAvp.getClass().getMethod(methodName);
					long value = (Long) m.invoke(usuAvp);

					if (value == Long.MIN_VALUE) {
						// It means the AVP was not present.. no null or NoSuchAvpException :(
						continue;
					}

					if (tracer.isInfoEnabled()) {
						tracer.info("[><] " + sidString + " Got " + value + " Used Units of type '" + type.toString() + "' ");
					}

					CreditControlUnit ccUnit = new CreditControlUnit();
					ccUnit.setUnitType(type);
					ccUnit.setUsedUnits(ccUnit.getUsedUnits() + value);

					// If we can find Reserved Units and Rate Information, let's fill with it
					for (CreditControlUnit reservedCCUnit : reservedCCUnits) {
						if (reservedCCUnit.getUnitType() == type) {
							// Copy the reserved amount from the last session into this session so that ABMF can update used units.
							ccUnit.setReservedUnits(reservedCCUnit.getReservedUnits());
							ccUnit.setReservedAmount(reservedCCUnit.getReservedAmount());

							ccUnit.setUsedAmount((long)Math.ceil(reservedCCUnit.getRateForService() * ccUnit.getUsedUnits()));
							ccUnit.setRateForService(reservedCCUnit.getRateForService());
						}
					}

					usedCCUnits.add(ccUnit);
				}
				catch (Exception e) {
					tracer.severe("[xx] " + sidString + " Unable to retrieve/invoke '" + methodName + "' for extracting Used Units of type " + type, e);
				}
			}
		}

		return usedCCUnits;
	}

	private static String toCamelCase(String s) {
		String[] parts = s.split("-");
		String camelCaseString = "";
		for (String part : parts){
			camelCaseString = camelCaseString + toProperCase(part);
		}
		return camelCaseString;
	}

	private String limitString(String str, int start, int end, String sep) {
		if(str.length() <= (start + end + sep.length())) {
			return str;
		}
		return str.substring(0, start) + sep + str.substring(str.length()-end);
	}

	private static String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	// 'sessionInfo' CMP field setter
	public abstract void setSessionInfo(UserSessionInfo value);

	// 'sessionInfo' CMP field getter
	public abstract UserSessionInfo getSessionInfo();

	// 'timerID' CMP field setter
	public abstract void setTimerID(TimerID value);

	// 'timerID' CMP field getter
	public abstract TimerID getTimerID();
}