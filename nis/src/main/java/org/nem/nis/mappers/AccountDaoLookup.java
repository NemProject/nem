package org.nem.nis.mappers;

import org.nem.nis.dbmodel.Account;
import org.nem.core.model.Address;

/**
 * An interface for looking up dao accounts.
 */
public interface AccountDaoLookup {

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 *
	 * @return The account with the specified id.
	 */
	public Account findByAddress(final Address id);
}
