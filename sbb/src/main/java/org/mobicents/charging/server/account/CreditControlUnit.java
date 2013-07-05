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

package org.mobicents.charging.server.account;

import java.io.Serializable;

import net.java.slee.resource.diameter.cca.events.avp.CcMoneyAvp;
import net.java.slee.resource.diameter.cca.events.avp.CcUnitType;

/**
 * Helper class for handling unit determination for each MSCC.
 * 
 * @author rsaranathan
 */
public class CreditControlUnit implements Serializable {

	private static final long serialVersionUID = -2984035448946896438L;

	/**
	 * [ CC-Time ]
     * [ CC-Money ]
     * [ CC-Total-Octets ]
     * [ CC-Input-Octets ]
     * [ CC-Output-Octets ]
     * [ CC-Service-Specific-Units ]
	 */
	private CcUnitType unitType;
	
	private long requestedUnits;
	
	private long requestedAmount;
	
	private long reservedUnits;
	
	private long reservedAmount;
	
	private long usedUnits;
	
	private long usedAmount;
	
	private double rateForService;
	
	// Details about the CC Money.
	private CcMoneyAvp ccMoney;
	
	public CcUnitType getUnitType() {
		return unitType;
	}

	public void setUnitType(CcUnitType unitType) {
		this.unitType = unitType;
	}

	public long getRequestedUnits() {
		return requestedUnits;
	}

	public void setRequestedUnits(long requestedUnits) {
		this.requestedUnits = requestedUnits;
	}

	public long getRequestedAmount() {
		return requestedAmount;
	}

	public void setRequestedAmount(long requestedAmount) {
		this.requestedAmount = requestedAmount;
	}

	public long getReservedUnits() {
		return reservedUnits;
	}

	public void setReservedUnits(long reservedUnits) {
		this.reservedUnits = reservedUnits;
	}

	public long getReservedAmount() {
		return reservedAmount;
	}

	public void setReservedAmount(long reservedAmount) {
		this.reservedAmount = reservedAmount;
	}

	public long getUsedUnits() {
		return usedUnits;
	}

	public void setUsedUnits(long usedUnits) {
		this.usedUnits = usedUnits;
	}

	public long getUsedAmount() {
		return usedAmount;
	}

	public void setUsedAmount(long usedAmount) {
		this.usedAmount = usedAmount;
	}

	public double getRateForService() {
		return rateForService;
	}

	public void setRateForService(double rateForService) {
		this.rateForService = rateForService;
	}
	
	public CcMoneyAvp getCcMoney() {
		return ccMoney;
	}

	public void setCcMoney(CcMoneyAvp ccMoney) {
		this.ccMoney = ccMoney;
	}

	@Override
	public String toString() {
	
		String ret = "CreditControlUnits[UnitType=" + unitType +
				"; RequestedUnits=" + requestedUnits +
				"; RequestedAmount=" + requestedAmount +
				"; ReservedUnits=" + reservedUnits +
				"; ReservedAmount=" + reservedAmount +
				"; UsedUnits=" + usedUnits +
				"; UsedAmount=" + usedAmount +
				"; RateForService=" + rateForService;
		if (ccMoney != null) {
			ret += "; CC-Money-AVP=" + ccMoney;
		}
		ret +=	"]";
		return ret;
		
	}

}
