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

package org.mobicents.charging.server.ratingengine;

import java.io.Serializable;

/**
 * Helper class for handling Rating Information
 * 
 * @author rsaranathan
 */
public class RatingInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private int responseCode;
	private String sessionId;
	private long actualTime;
	private long currentTime;
	private double rate;
	private String rateDescription;
	private String ratePromo;
	
	public RatingInfo(int responseCode, String sessionId){
		this.responseCode = responseCode;
		this.sessionId = sessionId;
	}
	
	public RatingInfo(int responseCode, String sessionId, long actualTime, long currentTime, double rate, String rateDescription, String ratePromo){
		this.responseCode = responseCode;
		this.sessionId = sessionId;
		this.actualTime = actualTime;
		this.currentTime = currentTime;
		this.rate = rate;
		this.rateDescription = rateDescription;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getActualTime() {
		return actualTime;
	}

	public void setActualTime(long actualTime) {
		this.actualTime = actualTime;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public String getRateDescription() {
		return rateDescription;
	}

	public void setRateDescription(String rateDescription) {
		this.rateDescription = rateDescription;
	}

	public String getRatePromo() {
		return ratePromo;
	}

	public void setRatePromo(String ratePromo) {
		this.ratePromo = ratePromo;
	}
	
	public String toString(){
		String ret = "Rating Info[" +
			"responseCode: "		+ responseCode + "; " +
			"sessionId: " 			+ sessionId + "; " +
			"actualTime: " 			+ actualTime + "; " +
			"currentTime: " 		+ currentTime + "; " +
			"sessionId: " 			+ rate + "; " +
			"rateDescription: " 	+ rateDescription + "; " +
			"ratePromo: " 			+ ratePromo + "; " +
			"]";

		return ret;
	}
}
