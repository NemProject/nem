package org.nem.nis;

import org.nem.core.model.HashCache;
import org.nem.nis.poi.PoiFacade;

/**
 * Class holding cached data.
 * TODO 20141204: since the AccountAnalyzer is really just a pair of (accountcache, poifacade),
 * > and this is a pair of that and transactionHashCache;
 * > i think we should flatten the structure so that this is really a triple of the three components)
 */
public class NisCache {
	private final AccountCache accountCache;
	private final PoiFacade poiFacade;
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
			final PoiFacade poiFacade,
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
	public NisCache copy()
	{
		return new NisCache(this.accountCache.copy(), this.poiFacade.copy(), this.transactionHashCache.copy());
	}

	/**
	 * Shallow copies the contents of this NIS cache to the specified cache.
	 *
	 * @param nisCache The other cache.
	 */
	public void shallowCopyTo(final NisCache nisCache) {
		this.accountCache.shallowCopyTo(nisCache.getAccountCache());
		this.poiFacade.shallowCopyTo(nisCache.getPoiFacade());
		this.transactionHashCache.shallowCopyTo(nisCache.getTransactionHashCache());
	}
}
