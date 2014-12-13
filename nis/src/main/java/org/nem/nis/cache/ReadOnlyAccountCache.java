package org.nem.nis.cache;

import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;

/**
 * A read only account cache.
 */
public interface ReadOnlyAccountCache extends AccountLookup {

	/**
	 * Gets the number of accounts.
	 *
	 * @return The number of accounts.
	 */
	public int size();

	/**
	 * Gets the contents of this cache.
	 *
	 * @return The cache contents.
	 */
	public CacheContents<Account> contents();
}