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

package org.mobicents.charging.server.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType;
import net.java.slee.resource.diameter.ro.events.RoCreditControlRequest;

import org.mobicents.charging.server.account.CreditControlInfo;

/**
 * POJO for keeping track of current user's session information
 * 
 * @author rsaranathan
 */
public class UserSessionInfo implements Serializable {

	private static final long serialVersionUID = -6258170300724976637L;

	private long sessionStartTime;
	
	private String sessionId;

	private SubscriptionIdType endUserType;

	private String endUserId;
	
	
	private RoCreditControlRequest ccr;
	
	private long[] serviceIds;
		
	/**
	 * List of Credit Controls for the session.
	 */
	private ArrayList<CreditControlInfo> reservations = new ArrayList<CreditControlInfo>();
	

	public long getSessionStartTime() {
		return sessionStartTime;
	}

	public void setSessionStartTime(long sessionStartTime) {
		this.sessionStartTime = sessionStartTime;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType getEndUserType() {
		return endUserType;
	}

	public void setEndUserType(
			net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType endUserType) {
		this.endUserType = endUserType;
	}

	public String getEndUserId() {
		return endUserId;
	}

	public void setEndUserId(String endUserId) {
		this.endUserId = endUserId;
	}

	public RoCreditControlRequest getCcr() {
		return ccr;
	}

	public void setCcr(RoCreditControlRequest ccr) {
		this.ccr = ccr;
	}

	public long[] getServiceIds() {
		return serviceIds;
	}

	public void setServiceIds(long[] serviceIds) {
		this.serviceIds = serviceIds;
	}

	public ArrayList<CreditControlInfo> getReservations() {
		return reservations;
	}

	public void setStoredReservations(ArrayList<CreditControlInfo> reservations) {
		this.reservations = reservations;
	}

	@Override
	public String toString() {
		String ret = "UserSessionInfo[" +
			"SessionStartTime=" + sessionStartTime + "; " +
			"SessionId=" + sessionId + "; " +
			"EndUserID=" + endUserId + "; " +
			"EndUserType=" + endUserType + "; " +
			"ServiceIDs=" + Arrays.toString(serviceIds) + ";";
		
		// This contains everything about the user's session. For debugging purposes only, need to format it better. 
		for (int i = 0; i < reservations.size(); i++) {
			CreditControlInfo ccInfo = reservations.get(i);
			ret += "\nSession " + (i+1) + ":" + ccInfo;
		}
		ret += "]";
		
		return ret;
	}
	
}
