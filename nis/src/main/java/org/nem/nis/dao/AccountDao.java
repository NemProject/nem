package org.nem.nis.dao;

import org.nem.nis.dbmodel.DbAccount;

/**
 * DAO for accessing DbAccount objects
 */
public interface AccountDao {
	/**
	 * Retrieves DbAccount from db given its id in the database.
	 *
	 * @param id id of a record.
	 * @return DbAccount associated with given id or null.
	 */
	DbAccount getAccount(Long id);

	/**
	 * Retrieves DbAccount from db given its printable (encoded) address.
	 *
	 * @param printableAddress NEM address
	 * @return DbAccount associated with given printableAddress or null.
	 */
	DbAccount getAccountByPrintableAddress(String printableAddress);

	/**
	 * Saves an account in the database.
	 * Note: if id wasn't set, it'll be filled after save()
	 *
	 * @param account DbAccount that's going to be saved.
	 */
	void save(DbAccount account);
}
