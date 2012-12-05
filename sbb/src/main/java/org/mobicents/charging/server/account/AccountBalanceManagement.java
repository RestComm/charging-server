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

/**
 * Interface with basic operations to manage user account units.
 * 
 * @author ammendonca
 * @author baranowb
 */
public interface AccountBalanceManagement {

	UnitReservation initialRequest(String sessionId, String userId, long requestedAmount);

	UnitReservation updateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber);

	UnitReservation terminateRequest(String sessionId, String userId, long requestedAmount, long usedAmount, int requestNumber);

	void dump();
	
	boolean addUser(String userId, long balance); 
}
