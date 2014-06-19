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

	/**
	 * Removes an account from the cache if it is in the cache.
	 *TODO: this should't be here, but I needed to add it to break core/nis dependency
	 *
	 * @param Address address The account id
	 *
	 */
	public void removeAccountFromCache(final Address id);
}
