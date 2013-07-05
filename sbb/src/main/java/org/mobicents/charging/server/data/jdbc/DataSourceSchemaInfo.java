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

/**
 * Class containing the schema information for the JDBC Datasource
 * 
 * @author ammendonca
 * @author rsaranathan
 */
public class DataSourceSchemaInfo {

	// --- SQL Tables and Columns Definition ----------------------------------

	public static final String _TBL_USERS = "MC_CS_USERS";

	public static final String _COL_MSISDN = "MSISDN";
	public static final String _COL_BALANCE = "BALANCE";
	public static final String _COL_BALANCE_EXPIRY_DATE = "BAL_EXPIRY_DATE";
	public static final String _COL_BAL_LAST_ADJUSTED = "BAL_LAST_ADJUSTED";
	public static final String _COL_USER_STATUS = "USER_STATUS";
	// --- SQL Queries --------------------------------------------------------

	public static final String _QUERY_EXISTS = "SELECT 1 FROM " + _TBL_USERS + ";";

	public static final String _QUERY_DROP = "DROP TABLE IF EXISTS " + _TBL_USERS + ";";

	public static final String _QUERY_CREATE = "CREATE TABLE " + _TBL_USERS
			+ " (" 
			+ _COL_MSISDN 				+ " VARCHAR(255) NOT NULL, "
			+ _COL_BALANCE 				+ " FLOAT NOT NULL, "
			+ _COL_BALANCE_EXPIRY_DATE 	+ " DATE NULL, "
			+ _COL_BAL_LAST_ADJUSTED 	+ " TIMESTAMP NULL, "
			+ _COL_USER_STATUS 			+ " VARCHAR(50) NOT NULL, "
			+ "PRIMARY KEY(" + _COL_MSISDN + ")" + ");";
	
	public static final String _QUERY_INSERT = "INSERT INTO " + _TBL_USERS
			+ " (" + _COL_MSISDN + ", " + _COL_BALANCE + ", " + _COL_BALANCE_EXPIRY_DATE + ", " + _COL_BAL_LAST_ADJUSTED + ", " + _COL_USER_STATUS + ")  VALUES (?, ?, ?, ?, ?)";

	public static final String _QUERY_SELECT = "SELECT * FROM " + _TBL_USERS + " WHERE " + _COL_MSISDN + " LIKE ?;";

	/*
	public static final String _QUERY_RESERVE = 
			"UPDATE " + _TBL_USERS +
			//                                          B = B + (G - U) - R
			" SET " + _COL_BALANCE + " = " + _COL_BALANCE + " + (" + _COL_RESERVED_AMOUNT + " - ?) - ?, " +
			//                                           G = G - U + R 
			//       _COL_RESERVED + " = " + _COL_RESERVED + " - ? + ? "+
			_COL_RESERVED_UNITS + " = " + " ?, "+
			_COL_RESERVED_AMOUNT + " = " + " ? "+
			//                                                                      B + (G - U) >= R
			"WHERE " + _COL_IMSI + " = ? AND " + _COL_BALANCE + " + (" + _COL_RESERVED_AMOUNT + " - ?) >= ?";
	*/

	/*
	public static final String _QUERY_RESERVE = 
			"UPDATE " + _TBL_USERS +
			" SET " + _COL_BALANCE + " = " + _COL_BALANCE + " + ? " +
			"WHERE " + _COL_MSISDN + " = ? AND " + _COL_BALANCE + " >= ?";
	*/
	
	public static final String _QUERY_RESERVE = 
			"UPDATE " + _TBL_USERS +
			//                                          B = B + (G - U) - R
			" SET " + _COL_BALANCE + " = " + _COL_BALANCE + " + ? - ? " +
			"WHERE " + _COL_MSISDN + " = ?";
	
	public static void main(String[] args) {
		System.out.println("Create Query: "+_QUERY_CREATE);
		System.out.println("Reserve Query: "+_QUERY_RESERVE);
		System.out.println("Insert Query: "+_QUERY_INSERT);
		System.out.println("Select Query: "+_QUERY_SELECT);
	}
}
