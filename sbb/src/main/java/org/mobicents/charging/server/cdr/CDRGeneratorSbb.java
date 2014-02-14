/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2014, TeleStax and individual contributors as indicated
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
package org.mobicents.charging.server.cdr;

import net.java.slee.resource.diameter.cca.events.avp.CcUnitType;
import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.account.CreditControlInfo;
import org.mobicents.charging.server.account.CreditControlUnit;
import org.mobicents.charging.server.data.UserSessionInfo;
import org.mobicents.slee.SbbContextExt;

import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Call Detail Record (CDR) Generator SBB.
 *
 * @author ammendonca
 */
public abstract class CDRGeneratorSbb extends BaseSbb implements Sbb, CDRGenerator {

	private Tracer tracer;
	private SbbContextExt sbbContextExt;

	public void setSbbContext(SbbContext context) {
		this.sbbContextExt = (SbbContextExt) context;
		this.tracer = sbbContextExt.getTracer("CS-CDRG");
	}

	public void unsetSbbContext() {
		this.sbbContextExt = null;
		this.tracer = null;
	}

	public void writeCDR(UserSessionInfo sessionInfo) {
		// Let's sum up the total used units and total used amount for the CDR.
		ArrayList<CreditControlInfo> reserv = sessionInfo.getReservations();

		long balanceBefore = 0;
		long balanceAfter = 0;
		long totalUsedUnitsInput = 0;
		long totalUsedUnitsMoney = 0;
		long totalUsedUnitsOutput = 0;
		long totalUsedUnitsServiceSpecific = 0;
		long totalUsedUnitsTime = 0;
		long totalUsedUnitsTotal = 0;
		long totalUsedAmountInput = 0;
		long totalUsedAmountMoney = 0;
		long totalUsedAmountOutput = 0;
		long totalUsedAmountServiceSpecific = 0;
		long totalUsedAmountTime = 0;
		long totalUsedAmountTotal = 0;

		for (int i = 0; i < reserv.size(); i++) {
			CreditControlInfo ccI = reserv.get(i);

			ArrayList<CreditControlUnit> ccUnits = ccI.getCcUnits();
			for (int j = 0; j < ccUnits.size(); j++) {
				CreditControlUnit ccUnit = ccUnits.get(j);
				if (ccUnit.getUnitType() == CcUnitType.INPUT_OCTETS) {
					totalUsedUnitsInput += ccUnit.getUsedUnits();
					totalUsedAmountInput += ccUnit.getUsedAmount();
				}
				if (ccUnit.getUnitType() == CcUnitType.MONEY) {
					totalUsedUnitsMoney += ccUnit.getUsedUnits();
					totalUsedAmountMoney += ccUnit.getUsedAmount();
				}
				if (ccUnit.getUnitType()==CcUnitType.OUTPUT_OCTETS) {
					totalUsedUnitsOutput += ccUnit.getUsedUnits();
					totalUsedAmountOutput += ccUnit.getUsedAmount();
				}
				if (ccUnit.getUnitType()==CcUnitType.SERVICE_SPECIFIC_UNITS) {
					totalUsedUnitsServiceSpecific += ccUnit.getUsedUnits();
					totalUsedAmountServiceSpecific += ccUnit.getUsedAmount();
				}
				if (ccUnit.getUnitType()==CcUnitType.TIME) {
					totalUsedUnitsTime += ccUnit.getUsedUnits();
					totalUsedAmountTime += ccUnit.getUsedAmount();
				}
				if (ccUnit.getUnitType()==CcUnitType.TOTAL_OCTETS) {
					totalUsedUnitsTotal += ccUnit.getUsedUnits();
					totalUsedAmountTotal += ccUnit.getUsedAmount();
				}
			}

			if (i == 0) {
				balanceBefore = ccI.getBalanceBefore();
			}
			if (i == reserv.size() - 1) {
				balanceAfter = ccI.getBalanceAfter();
			}
		}


		/**
		 * Date Time of record (Format: yyyy-MM-dd'T'HH:mm:ss.SSSZ)
		 * Diameter Origin Host
		 * Diameter Origin Realm
		 * Diameter Destination Host
		 * Diameter Destination Realm
		 * Service IDs
		 * Session Start Time
		 * Current Time in milliseconds
		 * Session Duration
		 * SessionID
		 * Calling party type
		 * Calling party info
		 * Called party type
		 * Called party info
		 * Balance Before
		 * Balance After
		 * Total Input Octets Units Used
		 * Total Input Octets Amount Charged
		 * Total Money Units Used
		 * Total Money Amount Charged
		 * Total Output Octets Units Used
		 * Total Output Octets Amount Charged
		 * Total Service Specific Units Used
		 * Total Service Specific Amount Charged
		 * Total Time Units Used
		 * Total Time Amount Charged
		 * Total Total Octets Units Used
		 * Total Total Octets Amount Charged
		 * Event Type - Create/Interim/Terminate/Event (CDR's are only generated at Terminate for now)
		 * Number of events in this session
		 * Termination Cause
		 **/
		String CDR = "";
		String DELIMITER = ";";

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		long elapsed = System.currentTimeMillis()-sessionInfo.getSessionStartTime();

		CDR += df.format(new Date()) 							+ DELIMITER;
		CDR += sessionInfo.getCcr().getOriginHost()				+ DELIMITER;
		CDR += sessionInfo.getCcr().getOriginRealm()			+ DELIMITER;
		CDR += sessionInfo.getCcr().getDestinationHost()		+ DELIMITER;
		CDR += sessionInfo.getCcr().getDestinationRealm()		+ DELIMITER;
		CDR += Arrays.toString(sessionInfo.getServiceIds())		+ DELIMITER;
		CDR += sessionInfo.getSessionStartTime()				+ DELIMITER;
		CDR += System.currentTimeMillis()					 	+ DELIMITER;
		CDR += elapsed										 	+ DELIMITER;
		CDR += sessionInfo.getCcr().getSessionId() 				+ DELIMITER;
		CDR += sessionInfo.getEndUserType().getValue()			+ DELIMITER;
		CDR += sessionInfo.getEndUserId()						+ DELIMITER;
		// TODO: Get Destination Subscription ID Type and Value if available
		CDR += sessionInfo.getEndUserType().getValue()			+ DELIMITER;
		CDR += sessionInfo.getEndUserId()						+ DELIMITER;
		CDR += balanceBefore									+ DELIMITER;
		CDR += balanceAfter										+ DELIMITER;
		CDR += totalUsedUnitsInput								+ DELIMITER;
		CDR += totalUsedAmountInput								+ DELIMITER;
		CDR += totalUsedUnitsMoney								+ DELIMITER;
		CDR += totalUsedAmountMoney								+ DELIMITER;
		CDR += totalUsedUnitsOutput								+ DELIMITER;
		CDR += totalUsedAmountOutput							+ DELIMITER;
		CDR += totalUsedUnitsServiceSpecific					+ DELIMITER;
		CDR += totalUsedAmountServiceSpecific					+ DELIMITER;
		CDR += totalUsedUnitsTime								+ DELIMITER;
		CDR += totalUsedAmountTime								+ DELIMITER;
		CDR += totalUsedUnitsTotal								+ DELIMITER;
		CDR += totalUsedAmountTotal								+ DELIMITER;
		// FIXME? CDR += storedCCR.getCcRequestType().getValue()			+ DELIMITER;
		CDR += sessionInfo.getReservations().size()				+ DELIMITER;
		// FIXME? CDR += storedCCR.getTerminationCause()					+ DELIMITER;

		// TODO: Use a different logger.
		tracer.info(CDR);
	}

	public void writeCDR(String message) {
		tracer.info(message);
	}
}
