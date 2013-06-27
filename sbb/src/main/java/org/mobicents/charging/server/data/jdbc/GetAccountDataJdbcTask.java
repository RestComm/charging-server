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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;

import org.mobicents.charging.server.account.AccountBalanceManagement;
import org.mobicents.charging.server.data.UserAccountData;
import org.mobicents.slee.resource.jdbc.task.JdbcTaskContext;

public class GetAccountDataJdbcTask extends DataSourceJdbcTask {

	private List<UserAccountData> accountDataList = null;

	private String msisdn;
	private Tracer tracer;

	public GetAccountDataJdbcTask(String msisdn, Tracer tracer) {
		this.msisdn = msisdn;
		this.tracer = tracer;
	}

	@Override
	public Object executeSimple(JdbcTaskContext taskContext) {
		try {
			PreparedStatement preparedStatement = taskContext.getConnection().prepareStatement(DataSourceSchemaInfo._QUERY_SELECT);
			preparedStatement.setString(1, msisdn);
			tracer.info(("[//] Executing DB Statement '" + DataSourceSchemaInfo._QUERY_SELECT).replaceFirst("\\?", msisdn));
			preparedStatement.execute();
			ResultSet resultSet = preparedStatement.getResultSet();
			accountDataList = new ArrayList<UserAccountData>();
			while (resultSet.next()) {
				UserAccountData accountData = new UserAccountData();
				accountData.setImsi(resultSet.getString(DataSourceSchemaInfo._COL_IMSI));
				accountData.setBalance(resultSet.getLong(DataSourceSchemaInfo._COL_BALANCE));
				accountData.setReserved(resultSet.getLong(DataSourceSchemaInfo._COL_RESERVED));
				accountDataList.add(accountData);
			}
		}
		catch (Exception e) {
			tracer.severe("Failed to execute task to get Account Data for MSIDN '" + msisdn + "'", e);
		}
		return this;
	}

	public List<UserAccountData> getAccountData() {
		return accountDataList;
	}

	@Override
	public void callBackParentOnException(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).getAccountDataResult(accountDataList);
	}

	@Override
	public void callBackParentOnResult(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).getAccountDataResult(accountDataList);
	}

}
