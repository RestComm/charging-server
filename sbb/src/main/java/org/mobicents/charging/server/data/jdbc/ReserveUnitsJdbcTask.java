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

package org.mobicents.charging.server.data.jdbc;

import java.sql.PreparedStatement;

import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;

import org.mobicents.charging.server.account.AccountBalanceManagement;
import org.mobicents.charging.server.account.UnitRequest;
import org.mobicents.charging.server.data.UserAccountData;
import org.mobicents.slee.resource.jdbc.task.JdbcTaskContext;

public class ReserveUnitsJdbcTask extends DataSourceJdbcTask {

	private UnitRequest unitRequest = null;
	private UserAccountData accountData = null;

	private String msisdn;
	private Long requestedUnits;
	private Long usedUnits;

	private Tracer tracer;

	public ReserveUnitsJdbcTask(UnitRequest unitRequest, Tracer tracer) {
		this.unitRequest = unitRequest;
		this.msisdn = unitRequest.getSubscriptionId();
		this.requestedUnits = unitRequest.getRequestedAmount();
		this.usedUnits = unitRequest.getUsedAmount();
		this.tracer = tracer;
	}

	@Override
	public Object executeSimple(JdbcTaskContext taskContext) {
		try {
			int n = 1;
			PreparedStatement preparedStatement = taskContext.getConnection().prepareStatement(DataSourceSchemaInfo._QUERY_RESERVE);
			preparedStatement.setLong(n++, usedUnits);
			preparedStatement.setLong(n++, requestedUnits);
			//preparedStatement.setLong(n++, usedUnits);
			preparedStatement.setLong(n++, requestedUnits);
			preparedStatement.setString(n++, msisdn);
			preparedStatement.setLong(n++, usedUnits);
			preparedStatement.setLong(n++, requestedUnits);
			tracer.info(("[//] Executing DB Statement '" + DataSourceSchemaInfo._QUERY_RESERVE).
					replaceFirst("\\?", usedUnits.toString()).
					replaceFirst("\\?", requestedUnits.toString()).
					replaceFirst("\\?", usedUnits.toString()).
					//replaceFirst("\\?", requestedUnits.toString()).
					replaceFirst("\\?", msisdn).
					replaceFirst("\\?", usedUnits.toString()).
					replaceFirst("\\?", requestedUnits.toString()));

			accountData = new UserAccountData();
			accountData.setImsi(msisdn);

			if (preparedStatement.executeUpdate() == 1) {
				// we are good, we have updated
				accountData.setBalance(Long.MAX_VALUE); // FIXME
				accountData.setReserved(requestedUnits);
				accountData.setFailure(false);
			}
			else {
				// check what kind of error happened
				accountData.setBalance(0);
				accountData.setReserved(0);
				accountData.setFailure(true);
			}
		}
		catch (Exception e) {
			tracer.severe("Failed to execute task to get Account Data for MSIDN '" + msisdn + "'", e);
		}
		return this;
	}

	public UserAccountData getAccountData() {
		return accountData;
	}

	@Override
	public void callBackParentOnException(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).reserveUnitsResult(unitRequest, accountData);
	}

	@Override
	public void callBackParentOnResult(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).reserveUnitsResult(unitRequest, accountData);
	}

}
