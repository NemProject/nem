package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public class ReadOnlyNisCache {
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
	public ReadOnlyNisCache(
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
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	public PoiFacade getPoiFacade() {
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
	 * Creates a deep copy of this NIS cache.
	 *
	 * @return The copy.
	 */
	public NisCache copy() {
		return null; //new ReadOnlyNisCache(this.accountCache.copy(), this.poiFacade.copy(), this.transactionHashCache.copy());
	}

	/**
	 * Shallow copies the contents of this NIS cache to the specified cache.
	 *
	 * @param nisCache The other cache.
	 */
	public void shallowCopyTo(final ReadOnlyNisCache nisCache) {
		this.accountCache.shallowCopyTo(nisCache.getAccountCache());
		this.poiFacade.shallowCopyTo(nisCache.poiFacade);
		this.transactionHashCache.shallowCopyTo(nisCache.getTransactionHashCache());
	}

	public void commit(final NisCache nisCache) {
	}
}
