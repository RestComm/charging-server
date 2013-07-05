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

import org.mobicents.charging.server.account.CreditControlInfo;

/**
 * Interface with operations to interact with datasource.
 * 
 * @author ammendonca
 * @author rsaranathan
 */
public interface DataSource {

	/**
	 * Initiates the data source
	 */
	public void init();

	/**
	 * Gets the user account data from the database, by msisdn
	 * 
	 * @param msisdn
	 */
	public void getUserAccountData(String msisdn);

	/**
	 * Places a new initial/update/terminate request for a user.
	 * 
	 * @param ccInfo
	 */
	public void requestUnits(CreditControlInfo ccInfo);

	/**
	 * Update user with specific msisdn. Overwrites balance.
	 * 
	 * @param msisdn
	 * @param balance
	 */
	public void updateUser(String msisdn, long balance);

}
