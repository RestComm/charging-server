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

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;
import javax.slee.transaction.SleeTransaction;

import org.mobicents.charging.server.DiameterChargingServer;
import org.mobicents.slee.resource.jdbc.task.JdbcTaskContext;

/**
 * 
 * @author ammendonca
 * @author rsaranathan
 */
public class UpdateUserJdbcTask extends DataSourceJdbcTask {

	private long balance;
	private String msisdn;
	private final Tracer tracer;

	public UpdateUserJdbcTask(String msisdn, Tracer tracer) {
		this.msisdn = msisdn;
		this.tracer = tracer;
	}

	public UpdateUserJdbcTask(String msisdn, long balance, Tracer tracer) {
		this.msisdn = msisdn;
		this.balance = balance;
		this.tracer = tracer;
	}

	@Override
	public Object executeSimple(JdbcTaskContext taskContext) {
		SleeTransaction tx = null;
		try {
			tx = taskContext.getSleeTransactionManager().beginSleeTransaction();
			Connection connection = taskContext.getConnection();
			// static value of query string, since its widely used :)
			PreparedStatement preparedStatement = connection.prepareStatement(DataSourceSchemaInfo._QUERY_INSERT);
			preparedStatement.setString(1, msisdn);
			preparedStatement.setFloat(2, balance);
			preparedStatement.setDate(3, null);
			preparedStatement.setTimestamp(4, null);
			preparedStatement.setString(5, "Active");
			int inserts = preparedStatement.executeUpdate();

			tx.commit();
			tx = null;

			return inserts;
		}
		catch (Exception e) {
			tracer.severe("Failed to execute jdbc task.", e);
			return null;
		}
		finally {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception f) {
					tracer.severe("failed to rollback tx", f);
				}
			}
		}
	}

	@Override
	public void callBackParentOnException(SbbLocalObject parent) {
		((DiameterChargingServer) parent).updateAccountDataResult(false);
	}

	@Override
	public void callBackParentOnResult(SbbLocalObject parent) {
		((DiameterChargingServer) parent).updateAccountDataResult(true);
	}

}
