package org.mobicents.charging.server.data.noop;

import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.account.CreditControlInfo;
import org.mobicents.charging.server.data.DataSource;

import javax.slee.Sbb;

/**
 * @author ammendonca
 */
public abstract class DataSourceNoOpSbb extends BaseSbb implements Sbb, DataSource {

	public void init() {
		// NO-OP
	}

	/**
	 * Gets the user account data from the database, by msisdn
	 *
	 * @param msisdn
	 */
	public void getUserAccountData(String msisdn) {
		// NO-OP
	}

	/**
	 * Places a new initial/update/terminate request for a user.
	 *
	 * @param ccInfo
	 */
	public void requestUnits(CreditControlInfo ccInfo) {
		// NO-OP
	}

	/**
	 * Places a new event/direct-debit request for a user.
	 *
	 * @param ccInfo
	 */
	public void directDebitUnits(CreditControlInfo ccInfo) {
		// NO-OP
	}

	public void updateUser(String msisdn, long balance) {
		// NO-OP
	}

}
