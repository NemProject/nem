package org.nem.nis.cache;

import org.nem.core.model.*;

/**
 * An account cache that maps addresses to public keys.
 */
public interface AccountCache extends ReadOnlyAccountCache {

	/**
	 * Adds an account to the cache if it is not already in the cache
	 *
	 * @param address The address of the account to add.
	 * @return The account.
	 */
	Account addAccountToCache(final Address address);

	/**
	 * Removes an account from the cache if it is in the cache.
	 *
	 * @param address The address of the account to remove.
	 */
	void removeFromCache(final Address address);
}
