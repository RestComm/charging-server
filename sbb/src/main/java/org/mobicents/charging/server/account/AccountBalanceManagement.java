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

import org.mobicents.charging.server.data.UserAccountData;

/**
 * Interface with basic operations to manage user account units.
 * 
 * @author ammendonca
 * @author baranowb
 */
public interface AccountBalanceManagement {

	/**
	 * Handler for CCR "INITIAL" Requests.
	 * 
	 * @param sessionId
	 * @param userId
	 * @param requestedAmount
	 */
	void initialRequest(String sessionId, String userId, long requestedAmount);

	/**
	 * Handler for CCR "UPDATE" Requests.
	 * 
	 * @param sessionId
	 * @param userId
	 * @param requestedAmount
	 * @param usedAmount
	 * @param requestNumber
	 */
	void updateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber);

	/**
	 * Handler for CCR "TERMINATE" Requests.
	 * 
	 * @param sessionId
	 * @param userId
	 * @param requestedAmount
	 * @param usedAmount
	 * @param requestNumber
	 */
	void terminateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber);

	/**
	 * Dump data from Database into console, filtering users by regular expression.
	 * 
	 * @param usersRegExp
	 */
	void dump(String usersRegExp);

	/**
	 * Allows for bypassing Unit reserve, etc. Useful for evaluating performance hit.
	 * 
	 * @param bypass
	 */
	void setBypass(boolean bypass);

	// Datasource

	/**
	 * Datasource callback method for getting user data from database.
	 * 
	 * @param uad
	 */
	public void getAccountDataResult(List<UserAccountData> uad);

	/**
	 * Datasource callback method for updating user data (reservation/termination) in database
	 * 
	 * @param unitRequest
	 * @param uad
	 */
	public void reserveUnitsResult(UnitRequest unitRequest, UserAccountData uad);

}
