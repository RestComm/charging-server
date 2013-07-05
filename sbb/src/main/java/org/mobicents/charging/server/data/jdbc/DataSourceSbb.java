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
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.slee.ActivityContextInterface;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.account.CreditControlInfo;
import org.mobicents.charging.server.data.DataSource;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.jdbc.JdbcActivity;
import org.mobicents.slee.resource.jdbc.JdbcActivityContextInterfaceFactory;
import org.mobicents.slee.resource.jdbc.JdbcResourceAdaptorSbbInterface;
import org.mobicents.slee.resource.jdbc.event.JdbcTaskExecutionThrowableEvent;
import org.mobicents.slee.resource.jdbc.task.simple.SimpleJdbcTask;
import org.mobicents.slee.resource.jdbc.task.simple.SimpleJdbcTaskResultEvent;

/**
 * Datasource Child SBB
 * 
 * This SBB is responsible for interacting with the Datasource, using the JDBC Resource Adaptor.
 * 
 * @author ammendonca
 * @author rsaranathan
 */
public abstract class DataSourceSbb extends BaseSbb implements Sbb, DataSource {

	/**
	 * the SBB object context
	 */
	private SbbContextExt sbbContextExt;

	/**
	 * the SBB logger
	 */
	private static Tracer tracer;

	// ------------------------------- JDBC RA --------------------------------
	private static final ResourceAdaptorTypeID jdbcRATypeID = JdbcResourceAdaptorSbbInterface.RATYPE_ID;
	private static final String jdbcRALink = "JDBCRA";
	private JdbcResourceAdaptorSbbInterface jdbcRA;
	private JdbcActivityContextInterfaceFactory jdbcACIF;

	// --------------------------- Local Interface ----------------------------

	@Override
	public void init() {
		// create db schema if needed
		Connection connection = null;
		boolean tableAlreadyExists = false;
		try {
			connection = jdbcRA.getConnection();
			tracer.info("[><] Got JDBC Connection");
			try { 
				PreparedStatement preparedStatement = connection.prepareStatement(DataSourceSchemaInfo._QUERY_EXISTS);
				preparedStatement.execute();
				ResultSet resultSet = preparedStatement.getResultSet();
				if (resultSet.next()) {
					tracer.info("[><] Table " + DataSourceSchemaInfo._TBL_USERS + " found in schema.");
					//tableAlreadyExists = true;
				}
			}
			catch (SQLException e) {
				// it's ok, maybe table does not exist. no need to do anything here. 
				tracer.warning("failed to query db schema", e);
			}
			
			if (!tableAlreadyExists) {
				connection.createStatement().execute(DataSourceSchemaInfo._QUERY_DROP);
				tracer.info("[><] Executed DROP Statement (" + DataSourceSchemaInfo._QUERY_DROP + ")");
	
				connection.createStatement().execute(DataSourceSchemaInfo._QUERY_CREATE);
				tracer.info("[><] Executed CREATE Statement (" + DataSourceSchemaInfo._QUERY_CREATE + ")");
			}
		}
		catch (SQLException e) {
			tracer.warning("failed to create db schema", e);
		}
		finally {
			try {
				connection.close();
				tracer.info("[><] Closed JDBC Connection");
			}
			catch (SQLException e) {
				tracer.severe("[xx] Failed to close JDBC Connection", e);
			}
		}
	}

	@Override
	public void getUserAccountData(String msisdn) {
		tracer.info("[><] Calling getUserAccountData(" + msisdn + ")");
		executeTask(new GetAccountDataJdbcTask(msisdn, tracer));
	}

	@Override
	public void requestUnits(CreditControlInfo ccInfo) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] Requesting Units: " + ccInfo);
		}
		executeTask(new ReserveUnitsJdbcTask(ccInfo, tracer));
	}

	@Override
	public void updateUser(String msisdn, long balance) {
		if (tracer.isInfoEnabled()) {
			tracer.info("[><] Updating User with MSISDN '" + msisdn + "'. Balance = " + balance);
		}
		executeTask(new UpdateUserJdbcTask(msisdn, balance, tracer));
	}

	// ---------------------------- Event Handlers ----------------------------

	/**
	 * Simple method to create JDBC activity and execute given task.
	 * 
	 * @param queryJDBCTask
	 */
	private void executeTask(SimpleJdbcTask jdbcTask) {
		JdbcActivity jdbcActivity = jdbcRA.createActivity();
		ActivityContextInterface jdbcACI = jdbcACIF.getActivityContextInterface(jdbcActivity);
		jdbcACI.attach(sbbContextExt.getSbbLocalObject());
		jdbcActivity.execute(jdbcTask);
	}

	/**
	 * Event handler for {@link JdbcTaskExecutionThrowableEvent}.
	 * 
	 * @param event
	 * @param aci
	 */

	public void onJdbcTaskExecutionThrowableEvent(JdbcTaskExecutionThrowableEvent event, ActivityContextInterface aci) {
		if (tracer.isWarningEnabled()) {
			tracer.warning("Received a JdbcTaskExecutionThrowableEvent, as result of executed task " + event.getTask(), event.getThrowable());
		}
		// end jdbc activity
		((JdbcActivity) aci.getActivity()).endActivity();
		// call back parent
		final SbbLocalObject parent = sbbContextExt.getSbbLocalObject().getParent();
		final DataSourceJdbcTask jdbcTask = (DataSourceJdbcTask) event.getTask();
		jdbcTask.callBackParentOnException(parent);
	}

	public void onSimpleJdbcTaskResultEvent(SimpleJdbcTaskResultEvent event, ActivityContextInterface aci) {
		if (tracer.isFineEnabled()) {
			tracer.fine("Received a SimpleJdbcTaskResultEvent, as result of executed task " + event.getTask());
		}
		// end jdbc activity
		((JdbcActivity) aci.getActivity()).endActivity();
		// call back parent
		final SbbLocalObject parent = sbbContextExt.getSbbLocalObject().getParent();
		final DataSourceJdbcTask jdbcTask = (DataSourceJdbcTask) event.getTask();
		jdbcTask.callBackParentOnResult(parent);
	}

	// ---------------------------- SLEE Callbacks ----------------------------

	@Override
	public void setSbbContext(SbbContext context) {
		sbbContextExt = (SbbContextExt) context;
		if (tracer == null) {
			tracer = sbbContextExt.getTracer("CS-JDBC");
		}
		jdbcRA = (JdbcResourceAdaptorSbbInterface) this.sbbContextExt.getResourceAdaptorInterface(jdbcRATypeID, jdbcRALink);
		jdbcACIF = (JdbcActivityContextInterfaceFactory) this.sbbContextExt.getActivityContextInterfaceFactory(jdbcRATypeID);
	}

	@Override
	public void unsetSbbContext() {
		sbbContextExt = null;
		jdbcRA = null;
		jdbcACIF = null;
	}
}
