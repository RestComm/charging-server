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

import java.util.concurrent.ConcurrentHashMap;

import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;

import org.mobicents.charging.server.BaseSbb;
import org.mobicents.slee.SbbContextExt;

/**
 * Child SBB for Account and Balance Management 
 * 
 * @author ammendonca
 * @author baranowb
 */
public abstract class AccountBalanceManagementSbb extends BaseSbb implements AccountBalanceManagement, Sbb/*Ext*/ {

	private Tracer tracer;
	private SbbContextExt sbbContext;

	private static ConcurrentHashMap<String, Long> userBalance;

	private static ConcurrentHashMap<String, Long> userUsedUnits = new ConcurrentHashMap<String, Long>();
	private static ConcurrentHashMap<String, Long> userReservedUnits = new ConcurrentHashMap<String, Long>();

	// --------------------------- SBB LO callbacks ---------------------------

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
	public UnitReservation initialRequest(String sessionId, String userId, long requestedAmount)
	{
		tracer.info(" [>>] Received an Initial Request to Account and Balance Management SBB.");

		// get user current balance 
		Long units = userBalance.get(userId);

		if (units == null) {
			return new UnitReservation(404L, "Invalid User", sessionId, System.currentTimeMillis());	
		}
		else {
			if (units <= 0) {
				return new UnitReservation(503L, "No Units Available", sessionId, System.currentTimeMillis());
			}
			else {
				return reserveUnits(sessionId, userId, requestedAmount, units);
			}
		}
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
	public  UnitReservation updateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber)
	{
		tracer.info(" [>>] Received an Update Request to Account and Balance Management SBB.");

		// get user current balance 
		Long units = userBalance.get(userId);

		checkUsedUnits(userId, usedAmount, units);

		if (units <= 0) {
			return new UnitReservation(503L, "No Units Available", sessionId, System.currentTimeMillis());
		}
		else {
			return reserveUnits(sessionId, userId, requestedAmount, units);
		}
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
	 */
	public UnitReservation terminateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber)
	{
		tracer.info(" [>>] Received an Update Request to Account and Balance Management SBB.");

		// get user current balance 
		Long units = userBalance.get(userId);

		checkUsedUnits(userId, usedAmount, units);

		return new UnitReservation(true, 0L, sessionId, System.currentTimeMillis());
	}

	// ------------------------------ SBB Entity ------------------------------

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

	public void dump() {
		tracer.info(String.format("%20s | %10s | %10s | %10s |",  "User ID", "Balance", "Reserved", "Used"));
		tracer.info("---------------------+------------+------------+------------+");
		for (String user : userBalance.keySet()) {
			tracer.info(String.format("%20s | %10s | %10s | %10s |",  user, userBalance.get(user), userReservedUnits.get(user),
					userUsedUnits.get(user)));
		}
	}

	public boolean addUser(String userId, long balance) {
		if (!userBalance.containsKey(userId)) {
			userBalance.put(userId, balance);
			return true;
		}
		return false;
	}
	
	// ---------------------------- Helper Methods ----------------------------

	private UnitReservation reserveUnits(String sessionId, String userId, long requestedAmount, long availableAmount) {
		long grantedUnits = Math.min(requestedAmount, availableAmount);

		// update balance
		userBalance.put(userId, availableAmount - grantedUnits);

		// update reserved units
		Long reserved = userReservedUnits.get(userId);
		if (reserved == null) {
			userReservedUnits.put(userId, grantedUnits);
		}
		else {
			userReservedUnits.put(userId, reserved + grantedUnits);
		}

		return new UnitReservation(true, grantedUnits, sessionId, System.currentTimeMillis());
	}

	private void checkUsedUnits(String userId, long usedAmount, long availableAmount) {
		Long reservedUnits = userReservedUnits.get(userId);

		Long nonUsedUnits = reservedUnits - usedAmount;

		if (nonUsedUnits < 0) {
			tracer.warning(" [!!] User used more than granted units.");
		}

		// update balance, if it makes sense
		if (nonUsedUnits != 0) {
			availableAmount += nonUsedUnits;
			userBalance.put(userId, availableAmount);
		}

		// update used units
		Long oldUsedUnits = userUsedUnits.get(userId);
		userUsedUnits.put(userId, oldUsedUnits != null ? oldUsedUnits + usedAmount : usedAmount);
	}

}
