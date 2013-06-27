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

import java.util.List;

import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.Tracer;

import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.DiameterChargingServer;
import org.mobicents.charging.server.account.UnitRequest.SubscriptionIdType;
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
 */
public abstract class AccountBalanceManagementSbb extends BaseSbb implements Sbb, AccountBalanceManagement {

	private Tracer tracer;
	private SbbContextExt sbbContext;

	// If set to true, no balance is verified for any user.
	private boolean bypass = false;

	// ---------------------------- Child Relations -----------------------------

	public abstract ChildRelation getDatasourceChildRelation();

	private static final String DATASOURCE_CHILD_NAME = "DATASOURCE";

	protected DataSource getDatasource() throws TransactionRequiredLocalException, IllegalArgumentException, NullPointerException, SLEEException, CreateException {
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
	 *  1. Get user balance
	 *  1.1. If balance is null, user does not exists. return.
	 *  2. Check if available units is at least greater than 0
	 *  2.1. If available units is equal or less than zero, send no available units message.
	 *  3. Grant the most possible units (either requested, or all the available if requested > available)
	 *  4. Subtract granted units from balance
	 *  5. Add granted units to reserved units
	 *  6. Return success
	 */
	public void initialRequest(String sessionId, String userId, long requestedAmount) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] SID<" + sessionId + "> Received an Initial Request to Account and Balance Management SBB.");
		}
		handleRequest(new UnitRequest(sessionId, SubscriptionIdType.END_USER_IMSI, userId, requestedAmount));
	}

	/*
	 * Update Request Handling
	 * 
	 *  1. Get user reserved units
	 *  2. Check difference between reserved and used units
	 *  2.1 If more than granted has been used, issue a warning... (do more?)
	 *  3. Get user balance
	 *  4. Update balance by adding unused units, if any. (or subtracting exceeded)
	 *  5. [Optional] Register used units, for informational purposes
	 *  6. Check if available units is at least greater than 0
	 *  6.1. If available units is equal or less than zero, send no available units message.
	 *  7. Grant the most possible units (either requested, or all the available if requested > available)
	 *  8. Subtract granted units from balance
	 *  9. Add granted units to reserved units
	 * 10. Return success
	 */
	public void updateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] SID<" + sessionId + "> Received an Update Request to Account and Balance Management SBB.");
		}
		handleRequest(new UnitRequest(sessionId, SubscriptionIdType.END_USER_IMSI, userId, requestedAmount, usedAmount));
	}

	/*
	 * Terminate Request Handling
	 * 
	 *  1. Get user reserved units
	 *  2. Check difference between reserved and used units
	 *  2.1 If more than granted has been used, issue a warning... (do more?)
	 *  3. Get user balance
	 *  4. Update balance by adding unused units, if any. (or subtracting exceeded)
	 *  5. [Optional] Register used units, for informational purposes
	 */
	public void terminateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[>>] SID<" + sessionId + "> Received an Terminate Request to Account and Balance Management SBB.");
		}
		handleRequest(new UnitRequest(sessionId, SubscriptionIdType.END_USER_IMSI, userId, requestedAmount, usedAmount));
	}

	/*
	 * We just call the Datasource method for getting user account data and once it has the data,
	 * callback will be called and user account data printed.
	 */
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

	// ---------------------------- Helper Methods ----------------------------

	private void handleRequest(UnitRequest unitRequest) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] SID<" + unitRequest.getSessionId() + "> Handling Credit-Control-Request.");
		}

		if (bypass) {
			if (tracer.isInfoEnabled()) {
				tracer.info("[><] SID<" + unitRequest.getSessionId() + "> Bypassing Unit Reservation...");
			}
			UnitReservation ur = new UnitReservation(true, unitRequest.getRequestedAmount(), unitRequest.getSessionId(), System.currentTimeMillis());
			((DiameterChargingServer)sbbContext.getSbbLocalObject().getParent()).resumeOnCreditControlRequest(ur);
		}

		// get user current balance
		DataSource ds = null;
		try {
			ds = getDatasource();
			ds.requestUnits(unitRequest);
		}
		catch (Exception e) {
			tracer.severe("[xx] Unable to obtain Datasource Child SBB", e);
		}
	}

	private void handleResponse(UnitRequest unitRequest, UserAccountData data) {
		// We got a response, so let's look at it
		UnitReservation ur = null;
		if (data.isFailure()) {
			if (data.getImsi() == null) {
				ur = new UnitReservation(404L, "Invalid User", unitRequest.getSessionId(), System.currentTimeMillis());	
			}
			else if (data.getReserved() > 0) {
				ur = new UnitReservation(503L, "No Units Available", unitRequest.getSessionId(), System.currentTimeMillis());
			}
		}
		else {
			ur = reserveUnits(unitRequest.getSessionId(), unitRequest.getSubscriptionId(), unitRequest.getRequestedAmount(), data.getReserved());
		}

		((DiameterChargingServer)sbbContext.getSbbLocalObject().getParent()).resumeOnCreditControlRequest(ur);
	}

	private UnitReservation reserveUnits(String sessionId, String userId, long requestedAmount, long availableAmount) {
		return new UnitReservation(true, requestedAmount, sessionId, System.currentTimeMillis());
	}

	// ---------------------- Datasource Child SBB Callbacks ------------------

	@Override
	public void getAccountDataResult(List<UserAccountData> result) {
		if (tracer.isInfoEnabled()) {
			tracer.info(String.format("%20s | %10s | %10s | %10s |", "User ID", "Balance", "Reserved", "Used"));
			tracer.info("---------------------+------------+------------+------------+");
			for (UserAccountData uad : result) {
				tracer.info(String.format("%20s | %10s | %10s | %10s |", uad.getImsi(), uad.getBalance(), uad.getReserved(), "N/A"));
			}
		}
	}

	@Override
	public void reserveUnitsResult(UnitRequest unitRequest, UserAccountData uad) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] SID<" + unitRequest.getSessionId() + "> Just received UPDATE callback from DataSource Child SBB.");
			tracer.info("[><] SID<" + unitRequest.getSessionId() + "> Received Reserve Units Result: " + uad + " (Request: " + unitRequest + ").");
		}
		handleResponse(unitRequest, uad);
	}

	public void setBypass(boolean bypass) {
		this.bypass = bypass;
	}

}
