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

package org.mobicents.charging.server.account;

import java.util.ArrayList;
import java.util.List;

import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.Tracer;


import net.java.slee.resource.diameter.cca.events.avp.RequestedActionType;
import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.DiameterChargingServer;
import org.mobicents.charging.server.data.DataSource;
import org.mobicents.charging.server.data.UserAccountData;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.SbbLocalObjectExt;

/**
 * Child SBB for Account and Balance Management 
 * 
 * @author ammendonca
 * @author baranowb
 * @author rsaranathan
 */
public abstract class AccountBalanceManagementSbb extends BaseSbb implements Sbb, AccountBalanceManagement {

	private Tracer tracer;
	private SbbContextExt sbbContext;

	// If set to true, no balance is verified for any user.
	private boolean bypass = false;

	// ---------------------------- Child Relations -----------------------------

	public abstract ChildRelation getDatasourceChildRelation();

	private static final String DATASOURCE_CHILD_NAME = "DATASOURCE";

	protected DataSource getDatasource() throws IllegalArgumentException, NullPointerException, SLEEException, CreateException {
		ChildRelationExt cre = (ChildRelationExt) getDatasourceChildRelation();
		SbbLocalObjectExt sbbLocalObject = cre.get(DATASOURCE_CHILD_NAME);
		if (sbbLocalObject == null) {
			sbbLocalObject = cre.create(DATASOURCE_CHILD_NAME);
		}

		return (DataSource) sbbLocalObject;
	}

	// ---------------------------- SLEE Callbacks ----------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
	 */
	public void setSbbContext(SbbContext context) {
		this.tracer = context.getTracer("CS-ABMF");
		this.sbbContext = (SbbContextExt) context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.Sbb#unsetSbbContext()
	 */
	public void unsetSbbContext() {
		this.sbbContext = null;
		this.tracer = null;
	}

	// ---------------------- SBB LocalObject Callbacks -----------------------

	/*
	 * Initial Request Handling
	 * 
	 */
	public void initialRequest(CreditControlInfo ccInfo){
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] Received an Initial Request to Account and Balance Management SBB. SessionId="+ccInfo.getSessionId()+", userId="+ccInfo.getSubscriptionId());
		}
		handleRequest(ccInfo);
	}

	/*
	 * Update Request Handling
	 * 
	 */
	public void updateRequest(CreditControlInfo ccInfo) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] Received an Update Request to Account and Balance Management SBB. SessionId="+ccInfo.getSessionId()+", userId="+ccInfo.getSubscriptionId());
		}
		handleRequest(ccInfo);
	}

	/*
	 * Terminate Request Handling
	 * 
	 */
	public void terminateRequest(CreditControlInfo ccInfo) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] Received a Terminate Request to Account and Balance Management SBB. SessionId="+ccInfo.getSessionId()+", userId="+ccInfo.getSubscriptionId());
		}
		handleRequest(ccInfo);
	}

	/*
	 * Event Request (IEC, service-type=4) Handling
	 * 
	 */
	public void eventRequest(CreditControlInfo ccInfo){
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] Received an Event Request to Account and Balance Management SBB. SessionId="+ccInfo.getSessionId()+", userId="+ccInfo.getSubscriptionId());
		}
		handleRequest(ccInfo);
	}

	public void dump(String usersRegExp) {
		if (tracer.isInfoEnabled()) {
			DataSource ds = null;
			try {
				ds = getDatasource();
				ds.getUserAccountData(usersRegExp);
			}
			catch (Exception e) {
				tracer.severe("[xx] Unable to obtain Datasource Child SBB", e);
			}
		}
	}
	
	public void dump(CreditControlInfo ccInfo, UserAccountData uad) {
		if (tracer.isInfoEnabled()) {
			tracer.info(String.format("%20s | %10s | %10s | %10s | %10s | %10s | %20s |", "MSISDN", "Balance", "Reserved Units", "Reserved Amount", "Used Units", "Used Amount", "Unit Type"));
			tracer.info("---------------------+------------+----------------+-----------------+------------+-------------+----------------------+");
			
			String msisdn = uad.getMsisdn();
			long bal = uad.getBalance();
			
			ArrayList<CreditControlUnit> ccUnits = ccInfo.getCcUnits();
			for (int i = 0; i < ccUnits.size(); i++) {
				CreditControlUnit ccUnit = ccUnits.get(i);
				long reserv = ccUnit.getReservedUnits();
				long reservAmt = ccUnit.getReservedAmount();
				long used = ccUnit.getUsedUnits();
				long usedAmt = ccUnit.getUsedAmount(); 				
				if (i == 0) {
					tracer.info(String.format("%20s | %10s | %14s | %15s | %10s | %11s | %20s |", msisdn, bal, reserv, reservAmt, used, usedAmt, ccUnit.getUnitType()));
				}
				else {
					tracer.info(String.format("%20s | %10s | %14s | %15s | %10s | %11s | %20s |", "", "", reserv, reservAmt, used, usedAmt, ccUnit.getUnitType()));
				}
			}
			
		}
	}

	// ---------------------------- Helper Methods ----------------------------

	private void handleRequest(CreditControlInfo ccInfo) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] SID<" + ccInfo.getSessionId() + "> Handling Credit-Control-Request...");
		}

		if (bypass) {
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] SID<" + ccInfo.getSessionId() + "> Bypassing Unit Reservation...");
			}
			
			ccInfo.setSuccess(true);
			((DiameterChargingServer)sbbContext.getSbbLocalObject().getParent()).resumeOnCreditControlRequest(ccInfo);
		}
		else {
			DataSource ds = null;
			try {
				ds = getDatasource();
				if (ccInfo.getRequestedAction() == RequestedActionType.DIRECT_DEBITING) {
					ds.directDebitUnits(ccInfo);
				}
				else {
					ds.requestUnits(ccInfo);
				}
			}
			catch (Exception e) {
				tracer.severe("[xx] Unable to obtain Datasource Child SBB", e);
			}
		}
	}

	private void handleResponse(CreditControlInfo ccInfo, UserAccountData data) {
		// We got a response, so let's look at it
		CreditControlInfo ccIA = ccInfo;
		
		//tracer.info("Unit Request: "+unitRequest+" UserAccountData: "+data);
		
		if (data != null) {
			if (data.isFailure()) {
				//ccIA.setSessionId(ccInfo.getSessionId());
				//ccIA.setEventTimestamp(System.currentTimeMillis());
				//ccIA.setSubscriptionId(ccInfo.getSubscriptionId());
				if (data.getMsisdn() == null) {
					ccIA.setErrorCode(CreditControlInfo.ErrorCodeType.InvalidUser.ordinal());
					ccIA.setErrorMessage("Invalid User");
				}
				else if (ccInfo.getCcUnits().get(0).getRequestedUnits() > 0) {
					ccIA.setErrorCode(CreditControlInfo.ErrorCodeType.NotEnoughBalance.ordinal());
					ccIA.setErrorMessage("No Units Available");
				}
				else{
					ccIA.setErrorCode(CreditControlInfo.ErrorCodeType.General.ordinal());
					ccIA.setErrorMessage("Other Error");
					// TODO: Expand response code list. Determine what else could cause number of rows updated to be <> 1 and return appropriate response.
				}
			} 
			else {
				ccIA = reserveUnits(ccInfo);
			}
		}
		else {
			//Data was null... JDBC issues? Need to handle appropriately.
		}
		
		((DiameterChargingServer)sbbContext.getSbbLocalObject().getParent()).resumeOnCreditControlRequest(ccIA);

		// Print the session info here.
		dump(ccIA, data);
	}

	private CreditControlInfo reserveUnits(CreditControlInfo ccInfo) {
		ccInfo.setSuccess(true);
		ccInfo.setEventTimestamp(System.currentTimeMillis());
		return ccInfo;
	}

	// ---------------------- Datasource Child SBB Callbacks ------------------

	@Override
	public void getAccountDataResult(List<UserAccountData> result) {
		if (tracer.isInfoEnabled()) {
			tracer.info(String.format("%20s | %10s |", "User ID", "Balance"));
			tracer.info("---------------------+------------+");
			for (UserAccountData uad : result) {
				tracer.info(String.format("%20s | %10s |", uad.getMsisdn(), uad.getBalance()));
			}			
		}
	}

	/**
	 * Callback method from JDBC
	 * @param ccInfo
	 * @param uad
	 */
	@Override
	public void reserveUnitsResult(CreditControlInfo ccInfo, UserAccountData uad) {
		if (tracer.isInfoEnabled()) {
			//tracer.info("[><] SID<" + ccInfo.getSessionId() + "> Just received UPDATE callback from DataSource Child SBB (Credit Control Info Result) Processing response...");
			tracer.info("[><] SID<" + ccInfo.getSessionId() + "> Received Credit Control Info Result: \n" + uad + "\n" + ccInfo);
		}
		handleResponse(ccInfo, uad);
	}

	public void setBypass(boolean bypass) {
		this.bypass = bypass;
	}
}
