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

import javax.slee.SbbLocalObject;
import javax.slee.facilities.Tracer;

import org.mobicents.charging.server.account.AccountBalanceManagement;
import org.mobicents.charging.server.account.CreditControlInfo;
import org.mobicents.charging.server.account.CreditControlUnit;
import org.mobicents.charging.server.data.UserAccountData;
import org.mobicents.slee.resource.jdbc.task.JdbcTaskContext;

/**
 * @author ammendonca
 * @author rsaranathan
 */
public class ReserveUnitsJdbcTask extends DataSourceJdbcTask {

	private CreditControlInfo ccInfo = null;
	private UserAccountData accountData = null;

	private String msisdn;
	private ArrayList<CreditControlUnit> ccUnits;

	private Tracer tracer;

	public ReserveUnitsJdbcTask(CreditControlInfo ccInfo, Tracer tracer) {
		this.ccInfo = ccInfo;
		this.msisdn = ccInfo.getSubscriptionId();
		this.ccUnits = ccInfo.getCcUnits();
		this.tracer = tracer;
	}

	@Override
	public Object executeSimple(JdbcTaskContext taskContext) {
		try {
			long balance = 0;
			// get Balance Before (can this be made more efficient?)
			PreparedStatement preparedStatement = taskContext.getConnection().prepareStatement(DataSourceSchemaInfo._QUERY_SELECT);
			preparedStatement.setString(1, msisdn);
			preparedStatement.execute();
			ResultSet resultSet = preparedStatement.getResultSet();
			while (resultSet.next()) {
				balance = resultSet.getLong(DataSourceSchemaInfo._COL_BALANCE);
				ccInfo.setBalanceBefore(balance);
			}
			
			for(int i=0; i<ccUnits.size();i++){
				CreditControlUnit ccUnit = ccUnits.get(i);
				if(balance<=0 && ccUnit.getRateForService()>0){
					accountData = new UserAccountData();
					accountData.setMsisdn(msisdn);
					accountData.setBalance(0);
					accountData.setFailure(true);
					ccUnit.setReservedUnits(0);
					ccUnit.setReservedAmount(0);
					if(tracer.isInfoEnabled()){
						tracer.info("[//] User does not have sufficient balance for reservation. Balance available: "+balance+".");
					}
					break;
				}else{
					long reservedAmount = ccUnit.getReservedAmount();
					long usedAmount = ccUnit.getUsedAmount();
					long requestedAmount = ccUnit.getRequestedAmount();
					long requestedUnits = ccUnit.getRequestedUnits();
					if(ccUnit.getRateForService()>0){
						//If RSU < balance, reserve and set GSU=balance	
						if((reservedAmount-usedAmount+requestedAmount)>balance){
							long newRequestedAmount = balance;
							long newRequestedUnits = (long) Math.floor(newRequestedAmount/ccUnit.getRateForService()); 
							if(tracer.isInfoEnabled()){
								tracer.info("[//] User does not have sufficient balance for the entire reservation request ("+requestedUnits+" " + ccUnit.getUnitType() + " units @rate="+ccUnit.getRateForService()+"). Balance available: "+balance+". Reserving "+newRequestedUnits+" units instead ...");
							}
							requestedAmount = newRequestedAmount;
							requestedUnits = newRequestedUnits;
							//TODO: Need to set Final Unit Indication for this case.
							// See http://www.ietf.org/rfc/rfc4006.txt, 8.34.  Final-Unit-Indication AVP
							
						}
					}
	
					int n = 1;
					tracer.info(("[//] Executing DB Statement '" + DataSourceSchemaInfo._QUERY_RESERVE).
							replaceFirst("\\?", String.valueOf(reservedAmount-usedAmount)).
							replaceFirst("\\?", String.valueOf(requestedAmount)).
							replaceFirst("\\?", msisdn)
							);
					preparedStatement = taskContext.getConnection().prepareStatement(DataSourceSchemaInfo._QUERY_RESERVE);
					preparedStatement.setLong(n++, (reservedAmount-usedAmount));
					preparedStatement.setLong(n++, requestedAmount);
					preparedStatement.setString(n++, msisdn);
		
					accountData = new UserAccountData();
					accountData.setMsisdn(msisdn);
		
					if (preparedStatement.executeUpdate() == 1) {
						// ok great, we have successfully reserved the units
						preparedStatement = taskContext.getConnection().prepareStatement(DataSourceSchemaInfo._QUERY_SELECT);
						preparedStatement.setString(1, msisdn);
						//tracer.info(("[//] Executing DB Statement '" + DataSourceSchemaInfo._QUERY_SELECT).replaceFirst("\\?", msisdn));
						preparedStatement.execute();
						resultSet = preparedStatement.getResultSet();
						while (resultSet.next()) {
							balance = resultSet.getLong(DataSourceSchemaInfo._COL_BALANCE);
							accountData.setBalance(balance);
							accountData.setFailure(false);
							ccUnit.setReservedUnits(requestedUnits);
							ccUnit.setReservedAmount(requestedAmount);
							ccInfo.setBalanceAfter(balance);
						}
					}
					else {
						// check what kind of error happened
						accountData.setBalance(0);
						accountData.setFailure(true);
						ccUnit.setReservedUnits(0);
						ccUnit.setReservedAmount(0);
					}
				}
			}
		}
		catch (Exception e) {
			tracer.severe("[xx] Failed to execute task to Reserve Units for MSISDN '" + msisdn + "'", e);
		}
		return this;
	}

	public UserAccountData getAccountData() {
		return accountData;
	}

	@Override
	public void callBackParentOnException(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).reserveUnitsResult(ccInfo, accountData);
	}

	@Override
	public void callBackParentOnResult(SbbLocalObject parent) {
		((AccountBalanceManagement) parent).reserveUnitsResult(ccInfo, accountData);
	}
}