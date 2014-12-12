package org.nem.nis.cache;

import org.nem.nis.state.ReadOnlyAccountState;

/**
 * The central point for accessing NIS-mutable data.
 */
public class NisCache {
	private final AccountCache accountCache;
	private final SynchronizedPoiFacade poiFacade;
	private final HashCache transactionHashCache;

	/**
	 * Creates a NIS cache from an existing account cache, a poi facade and a transaction hash cache.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @param transactionHashCache the transaction hash cache.
	 */
	public NisCache(
			final AccountCache accountCache,
			final SynchronizedPoiFacade poiFacade,
			final HashCache transactionHashCache) {
		this.accountCache = accountCache;
		this.poiFacade = poiFacade;
		this.transactionHashCache = transactionHashCache;
	}

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	public AccountCache getAccountCache() {
		return this.accountCache;
	}

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	public AccountStateRepository getPoiFacade() {
		return this.poiFacade;
	}

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	public HashCache getTransactionHashCache() {
		return this.transactionHashCache;
	}

	/**
	 * Creates a read-only representation of this cache.
	 *
	 * @return A read-only representation of this cache.
	 */
	public ReadOnlyNisCache asReadOnly() {
		return new ReadOnlyNisCache(
				this.accountCache,
				this.poiFacade,
				this.transactionHashCache);
	}

	public void commit(final ReadOnlyAccountState accountState) {

	}
}