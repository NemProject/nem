package org.nem.nis.dao;

import org.nem.nis.dbmodel.DbAccount;

/**
 * DAO for accessing DbAccount objects
 */
public interface AccountDao {

	/**
	 * Retrieves DbAccount from db given its printable (encoded) address.
	 *
	 * @param printableAddress NEM address
	 * @return DbAccount associated with given printableAddress or null.
	 */
	DbAccount getAccountByPrintableAddress(String printableAddress);
}
