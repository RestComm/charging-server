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

import net.java.slee.resource.diameter.cca.events.avp.RequestedActionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class for handling unit reservation
 * 
 * @author ammendonca
 * @author baranowb
 * @author rsaranathan
 */
public class CreditControlInfo implements Serializable {

	private static final long serialVersionUID = -382210507958956695L;

	public enum SubscriptionIdType {

		END_USER_E164(0),
		END_USER_IMSI(1),
		END_USER_SIP_URI(2),
		END_USER_NAI(3),
		END_USER_PRIVATE(4);

		private int value;

		private SubscriptionIdType(int value) {
			this.value = value;
		}

		public static SubscriptionIdType fromInt(int presumableValue) throws IllegalArgumentException {
			switch (presumableValue) {
			case 0:
				return END_USER_E164;
			case 1:
				return END_USER_IMSI;
			case 2:
				return END_USER_SIP_URI;
			case 3:
				return END_USER_NAI;
			case 4:
				return END_USER_PRIVATE;

			default:
				throw new IllegalArgumentException();
			}
		}

		public int getValue() {
			return this.value;
		}
	}

	private ErrorCodeType errorCodeType;

	public enum ErrorCodeType {
		General,
		// 100+
		MalformedRequest,
		// 200+
		InvalidUser, InvalidContent,
		// 300+
		BadRoamingCountry, NotEnoughBalance, NoServiceForUser,
		// 400+
		AccountingConnectionErr;

		ErrorCodeType() {
		}

		public final static ErrorCodeType getFromInt(int code) {
			switch (code) {
			case 100:
			case 101:
			case 102:
			case 103:
			case 104:
				return MalformedRequest;
			case 201:
				return InvalidUser;
			case 202:
				return InvalidContent;
			case 301:
				return BadRoamingCountry;
			case 302:
				return NotEnoughBalance;
			case 303:
				return NoServiceForUser;
			case 401:
			case 402:
			case 403:
				return AccountingConnectionErr;

			default:
				return General;
			}
		}
	}
	
	private String sessionId = "";
	
	private int requestNumber;
	private RequestedActionType requestedAction;

	// Subscription-Id Type and Data
	private net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType subscriptionIdType;
	private String subscriptionId = "";

	private ArrayList<CreditControlUnit> ccUnits;
	
	private long eventTimestamp;
	
	private String eventType = "";	// Initial/Interim/Terminate/Event
	
	private boolean success;
	
	private long errorCode;

	private String errorMessage = "";
	
	private long balanceBefore;
	
	private long balanceAfter;

	public ErrorCodeType getErrorCodeType() {
		return errorCodeType;
	}

	public void setErrorCodeType(ErrorCodeType errorCodeType) {
		this.errorCodeType = errorCodeType;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public int getRequestNumber() {
		return requestNumber;
	}

	public void setRequestNumber(int requestNumber) {
		this.requestNumber = requestNumber;
	}

	public RequestedActionType getRequestedAction() {
		return requestedAction;
	}

	public void setRequestedAction(RequestedActionType requestedAction) {
		this.requestedAction = requestedAction;
	}

	public net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType getSubscriptionIdType() {
		return subscriptionIdType;
	}

	public void setSubscriptionIdType(net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType subscriptionIdType) {
		this.subscriptionIdType = subscriptionIdType;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public long getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(long eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public boolean isSuccessful() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public long getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(long errorCode) {
		this.errorCode = errorCode;
		this.errorCodeType = ErrorCodeType.getFromInt((int) errorCode);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public long getBalanceBefore() {
		return balanceBefore;
	}

	public void setBalanceBefore(long balanceBefore) {
		this.balanceBefore = balanceBefore;
	}

	public long getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(long balanceAfter) {
		this.balanceAfter = balanceAfter;
	}
	
	public ArrayList<CreditControlUnit> getCcUnits() {
		return ccUnits;
	}

	public void setCcUnits(ArrayList<CreditControlUnit> ccUnits) {
		this.ccUnits = ccUnits;
	}

	// Support for service specific values
	HashMap<String, Object> serviceInfo = new HashMap<String, Object>();

	public Object addServiceInfo(String name, Object value) {
		return serviceInfo.put(name, value);
	}

	public Object removeServiceInfo(String name) {
		return serviceInfo.remove(name);
	}

	public Object getServiceInfo(String name) {
		return serviceInfo.get(name);
	}

	@Override
	public String toString() {
		String ret = "CreditControlInfo[Event-Timestamp=" + eventTimestamp +
				"; Event-Type=" + eventType +
				"; Session-ID=" + sessionId +
				"; Request-Number=" + requestNumber +
				"; Subscription-ID-Type=" + subscriptionIdType +
				"; Subscription-ID=" + subscriptionId +
				"; Balance-Before=" + balanceBefore +
				"; Balance-After=" + balanceAfter +
				"; Success=" + success;
		
		if (errorCode > 0) {
			ret += "; Error-Code=" + errorCode;
		}
		if (errorMessage != null && errorMessage.length() > 1) {
			ret += "; Error-Message=" + errorMessage;
		}
		// This contains everything about the user's session. For debugging purposes only, need to format it better.
		if (ccUnits != null) {
			for (int i = 0; i < ccUnits.size(); i++) {
				CreditControlUnit ccUnit = ccUnits.get(i);
				ret += "\n" + ccUnit;
			}
		}
		ret += "]";
		
		return ret;
	}
}
