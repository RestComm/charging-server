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
 * @author rsaranathan
 */
public interface AccountBalanceManagement {

	/**
	 * Handler for CCR "INITIAL" Requests.
	 * 
	 * @param ccInfo CreditControlInfo
	 */
	void initialRequest(CreditControlInfo ccInfo);

	/**
	 * Handler for CCR "UPDATE" Requests.
	 * 
	 * @param ccInfo CreditControlInfo
	 */
	void updateRequest(CreditControlInfo ccInfo);

	/**
	 * Handler for CCR "TERMINATE" Requests.
	 * 
	 * @param ccInfo CreditControlInfo
	 */
	void terminateRequest(CreditControlInfo ccInfo);
	
	/**
	 * Handler for CCR "EVENT" Requests.
	 * 
	 * @param ccInfo CreditControlInfo
	 */
	void eventRequest(CreditControlInfo ccInfo);

	/**
	 * Dump data from Database into console, filtering users by regular expression.
	 * 
	 * @param usersRegExp
	 */
	void dump(String usersRegExp);

	/**
	 * Dump Credit Control Session information on to the console for that specific user.
	 * 
	 * @param ccInfo
	 * @param uad
	 */
	void dump(CreditControlInfo ccInfo, UserAccountData uad);
	
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
	 * @param ccInfo
	 * @param uad
	 */
	public void reserveUnitsResult(CreditControlInfo ccInfo, UserAccountData uad);

}
