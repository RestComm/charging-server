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

import java.io.Serializable;

/**
 * Helper class for handling unit reservation
 * 
 * @author ammendonca
 * @author baranowb
 */
public class UnitReservation implements Serializable {

	private static final long serialVersionUID = 1L;

	private final boolean success;
	private final long errorCode;
	private final String errorMessage;
	private final long units;
	private final String sessionId;
	private final long timeStamp;

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

	/**
	 * @param success
	 * @param units
	 * @param sessionId
	 * @param timeStamp
	 */
	public UnitReservation(boolean success, long units, String sessionId, long timeStamp) {
		super();
		this.success = success;
		this.units = units;
		this.sessionId = sessionId;
		this.timeStamp = timeStamp;

		this.errorCodeType = null;
		this.errorCode = 0;
		this.errorMessage = null;
	}

	/**
	 * @param errorCode
	 * @param errorMessage
	 * @param sessionId
	 * @param timeStamp
	 */
	public UnitReservation(long errorCode, String errorMessage, String sessionId, long timeStamp) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.sessionId = sessionId;
		this.timeStamp = timeStamp;

		this.errorCodeType = ErrorCodeType.getFromInt((int) errorCode);
		this.success = false;
		this.units = 0;
	}

	/**
	 * @param success
	 * @param errorCode
	 * @param errorMessage
	 * @param units
	 * @param sessionId
	 * @param timeStamp
	 */
	public UnitReservation(boolean success, long errorCode, String errorMessage, long units, String sessionId, long timeStamp) {
		super();
		this.success = success;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.units = units;
		this.sessionId = sessionId;
		this.timeStamp = timeStamp;
		if (!success) {
			this.errorCodeType = ErrorCodeType.getFromInt((int) errorCode);
		}
	}

	public boolean isSuccess() {
		return success;
	}

	public long getErrorCode() {
		return errorCode;
	}

	public ErrorCodeType getErrorCodeType() {
		return errorCodeType;
	}

	public long getUnits() {
		return units;
	}

	public String getSessionId() {
		return sessionId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String toString() {
		String ret = "Unit Reservation[session-id: " + sessionId + "; success: " + success + "; ";

		if (errorCode != 0) {
			ret += "error code: " + errorCode;
			if (errorMessage != null) {
				ret += "/" + errorMessage;
			}
			ret += ";";
		}
		
		if (units != 0) {
			ret += "units: " + units + ";";
		}

		ret += "]";

		return ret;
	}
}
