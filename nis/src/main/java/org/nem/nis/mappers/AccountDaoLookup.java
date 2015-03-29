package org.nem.nis.mappers;

import org.nem.core.model.Address;
import org.nem.nis.dbmodel.DbAccount;

/**
 * An interface for looking up dao accounts.
 */
public interface AccountDaoLookup {

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 * @return The account with the specified id.
	 */
	DbAccount findByAddress(final Address id);
}
