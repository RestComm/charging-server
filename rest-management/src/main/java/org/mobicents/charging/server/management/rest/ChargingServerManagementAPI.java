package org.mobicents.charging.server.management.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.mobicents.charging.server.management.rest.json.ResultSetConverter;

@Path("/charging")
public class ChargingServerManagementAPI {

	Logger logger = LoggerFactory.getLogger("CS-REST");

	private static final String DS_CONTEXT = "java:/DefaultDS";

	private static final String USERS_TABLE = "CONCHA_USERS";

	private static DataSource datasource = null;

	private Connection getConnection() {
		Connection connection = null;

		Context initialContext;
		try {
			initialContext = new InitialContext();

			if (datasource == null) {
				datasource = (DataSource) initialContext.lookup(DS_CONTEXT);
			}

			if (datasource != null) {
				connection = datasource.getConnection();
			}
			else {
				logger.error("Failed to get connection to datasource (null).");
			}
		}
		catch (NamingException e) {
			logger.error("Unable to get JNDI InitialContext", e);
		}
		catch (SQLException e) {
			logger.error("Failed to get connection to datasource.", e);
		}

		return connection;
	}

    /**
     * Fetch All Users
     * [GET] http://mob-chaser/api/[version]/charging/users[?filter]
     *
     * @return a list of all the users
     */
	@GET
	@Path("/users")
	public Response getUsers() {
		String result = "Listing all users...";
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
		Connection connection = getConnection();
		try {
			ResultSet rSet = connection.createStatement().executeQuery("SELECT * FROM " + USERS_TABLE);
			result = ResultSetConverter.convert(rSet).toString();
		}
		catch (Exception e) {
			logger.error("Unable to execute SQL statement.", e);
		}
		finally {
			try {
				connection.close();
			}
			catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
			}
		}

		return Response.status(200).entity(result).build();
	}

    /**
     * Set User Balance
     * [PUT] http://mob-chaser/api/[version]/charging/users/msisdn/{msisdn}/balance/{value}
     *
     * @param msisdn the MSISDN of the user
     * @param value the balance value to set for the user
     * @return the result of the operation, as a text string
     */
	@POST
	@Path("/users/msisdn/{msisdn}/balance/{value}")
	public Response setUserBalance(@PathParam("msisdn") String msisdn, @PathParam("value") Long value) {
		String result = "Setting USER '" + msisdn + "' balance to " + value;
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
		Connection connection = getConnection();
		try {
            PreparedStatement ps = connection.prepareStatement("UPDATE " + USERS_TABLE + " SET BALANCE = ? WHERE MSISDN = ?");
            ps.setLong(1, value);
            ps.setString(2, msisdn);
            int updated = ps.executeUpdate();

			result = (updated == 1 ? "OK" : "FAIL");
		}
		catch (Exception e) {
			logger.error("Unable to execute SQL statement.", e);
		}
		finally {
			try {
				connection.close();
			}
			catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
			}
		}

		return Response.status(200).entity(result).build();
	}

    /**
     * Set User Reserved
     * [PUT] http://mob-chaser/api/[version]/charging/users/msisdn/{msisdn}/reserved/{value}
     *
	 * @param msisdn the MSISDN of the user
	 * @param value the reserved value to set for the user
     * @return the result of the operation, as a text string
     */
	@POST
	@Path("/users/msisdn/{msisdn}/reserved/{value}")
	public Response setUserReserved(@PathParam("msisdn") String msisdn, @PathParam("value") Long value) {
		String result = "Setting USER '" + msisdn + "' reserved to " + value;
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
		Connection connection = getConnection();
		try {
			int updated = connection.createStatement().executeUpdate("UPDATE " + USERS_TABLE + " SET RESERVED = " + value + " WHERE MSISDN = " + msisdn);
			result = (updated == 1 ? "OK" : "FAIL");
		}
		catch (Exception e) {
			logger.error("Unable to execute SQL statement.", e);
		}
		finally {
			try {
				connection.close();
			}
			catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
			}
		}

		return Response.status(200).entity(result).build();
	}

    /**
     * Sanitize User Balance (Move Reserved to Balance)
     * [POST] http://mob-chaser/api/[version]/charging/users/msisdn/{msisdn}/sanitize
     *
	 * @param msisdn the MSISDN of the user
     * @return the result of the operation, as a text string
     */
    @POST
    @Path("/users/msisdn/{msisdn}/sanitize")
    public Response sanitizeUserBalance(@PathParam("msisdn") String msisdn) {
        String result = "Sanitizing USER '" + msisdn + "'";
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
        Connection connection = getConnection();
        try {
            int updated = connection.createStatement().executeUpdate("UPDATE " + USERS_TABLE + " SET BALANCE = BALANCE + RESERVED, RESERVED = 0 WHERE MSISDN = " + msisdn);
            result = (updated == 1 ? "OK" : "FAIL");
        }
        catch (Exception e) {
			logger.error("Unable to execute SQL statement.", e);
        }
        finally {
            try {
                connection.close();
            }
            catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
            }
        }

        return Response.status(200).entity(result).build();
    }

    /**
     * Remove User
     * [DELETE] http://mob-chaser/api/[version]/charging/users/msisdn/{msisdn}/suspend
     *
	 * @param msisdn the MSISDN of the user
     * @return the result of the operation, as a text string
     */
    @DELETE
    @Path("/users/msisdn/{msisdn}")
    public Response deleteUser(@PathParam("msisdn") String msisdn) {
        String result = "Deleting USER '" + msisdn + "'";
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
        Connection connection = getConnection();
        try {
            // TODO: SELECT first so that we can return the deleted user information ?
            int updated = connection.createStatement().executeUpdate("DELETE FROM " + USERS_TABLE + " WHERE MSISDN = " + msisdn);
            result = (updated == 1 ? "OK" : "FAIL");
        }
        catch (Exception e) {
			logger.error("Unable to execute SQL statement.", e);
        }
        finally {
            try {
                connection.close();
            }
            catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
            }
        }

        return Response.status(200).entity(result).build();
    }

    /**
     * Add New User
     * [PUT] http://concha.mobicents.org/api/[version]/charging/users/msisdn/{msisdn}[/balance/{value}]
     *
	 * @param msisdn the MSISDN of the user
     * @return the result of the operation, as a text string
     */
    @PUT
    @Path("/users/msisdn/{msisdn}")
    public Response createUser(@PathParam("msisdn") String msisdn) {
        return createUser(msisdn, 0L);
    }

    /**
     * Add New User
     * [PUT] http://concha.mobicents.org/charging-server-rest-management/api/charging/users/msisdn/{msisdn}[/balance/{balance}]
     *
	 * @param msisdn the MSISDN of the new user
	 * @param balance the balance value to set for the new user
     * @return the result of the operation, as a text string
     */
    @PUT
    @Path("/users/msisdn/{msisdn}/balance/{balance}")
    public Response createUser(@PathParam("msisdn") String msisdn, @PathParam("balance") Long balance) {
        String result = "Adding USER '" + msisdn + "' with balance to " + balance;
		if (logger.isInfoEnabled()) {
			logger.info("[><] " + result);
		}
        Connection connection = getConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + USERS_TABLE + " (MSISDN, BALANCE, RESERVED, USER_STATUS) VALUES (?, ?, ?, ?)");
            ps.setString(1, msisdn);
            ps.setLong(2, balance);
			ps.setLong(3, 0);
			ps.setString(4, "ACTIVE");
            int updated = ps.executeUpdate();

            result = (updated == 1 ? "OK" : "FAIL");
        }
        catch (Exception e) {
            logger.error("Unable to execute SQL statement.", e);
        }
        finally {
            try {
                connection.close();
            }
            catch (Exception e) {
				logger.error("Failure trying to close connection.", e);
            }
        }

        return Response.status(200).entity(result).build();
    }

}

