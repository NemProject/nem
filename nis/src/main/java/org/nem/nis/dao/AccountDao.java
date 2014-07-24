package org.nem.nis.dao;

import org.nem.nis.dbmodel.Account;

/**
 * DAO for accessing db Account objects
 */
public interface AccountDao {
	/**
	 * Retrieves Account from db given it's id in the database.
	 *
	 * @param id id of a record.
	 *
	 * @return associated Account or null if there isn't Account with such id.
	 */
	public Account getAccount(Long id);

	/**
	 * Retrieves Account from db given it's printable (encoded) address.
	 *
	 * @param printableAddress NEM address
	 *
	 * @return Account associated with given printableAddress or null.
	 */
	public Account getAccountByPrintableAddress(String printableAddress);

	/**
	 * Saves an account in the database.
	 * Note: if id wasn't set, it'll be filled after save()
	 *
	 * @param account Account that's going to be saved.
	 */
	public void save(Account account);
}
