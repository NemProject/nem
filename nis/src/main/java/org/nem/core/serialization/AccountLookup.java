package org.nem.core.serialization;

import org.nem.core.model.Address;
import org.nem.core.model.Account;

/**
 * An interface for looking up accounts.
 */
public interface AccountLookup {

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 *
	 * @return The account with the specified id.
	 */
	public Account findByAddress(final Address id);

	/**
	 * Checks if an account is known.
	 *
	 * @param id The account id.
	 *
	 * @return true if the account is known, false if unknown.
	 */
	public boolean isKnownAddress(final Address id);

}
