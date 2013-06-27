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

/**
 * Helper class for handling unit request.
 * 
 * @author ammendonca
 */
public class UnitRequest {

	private String sessionId;

	// Subscription-Id Type and Data 
	private SubscriptionIdType subscriptionIdType;
	private String subscriptionId;

	// TODO request type (time, octets, money, etc)
	private long requestedAmount;
	private long usedAmount;

	public UnitRequest(String sessionId, SubscriptionIdType subscriptionIdType, String subscriptionId, long requestedAmount) {
		this.sessionId = sessionId;
		this.subscriptionIdType = subscriptionIdType;
		this.subscriptionId = subscriptionId;
		this.requestedAmount = requestedAmount;
	}

	public UnitRequest(String sessionId, SubscriptionIdType subscriptionIdType, String subscriptionId, long requestedAmount, long usedAmount) {
		this.sessionId = sessionId;
		this.subscriptionIdType = subscriptionIdType;
		this.subscriptionId = subscriptionId;
		this.requestedAmount = requestedAmount;
		this.usedAmount = usedAmount;
	}

	public String getSessionId() {
		return sessionId;
	}

	public SubscriptionIdType getSubscriptionIdType() {
		return subscriptionIdType;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public long getRequestedAmount() {
		return requestedAmount;
	}

	public long getUsedAmount() {
		return usedAmount;
	}

	@Override
	public String toString() {
		return "UnitRequest[Session-ID=" + sessionId + 
				"; Subscription-ID-Type=" + subscriptionIdType + 
				"; Subscription-ID=" + subscriptionId + 
				"; Requested-Amount=" + requestedAmount + "]";
	}

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

}
