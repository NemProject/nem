package org.nem.core.model;

import org.nem.nis.AccountAnalyzer;

/**
 * Class holding cached data.
 */
public class NisCache {
	private final AccountAnalyzer accountAnalyzer;
	private final HashCache transactionHashCache;

	/**
	 * Creates a NIS cache from an existing account analyzer and a transaction hash cache.
	 *
	 * @param accountAnalyzer The account analyzer.
	 * @param transactionHashCache the transaction hash cache.
	 */
	public NisCache(final AccountAnalyzer accountAnalyzer, final HashCache transactionHashCache) {
		this.accountAnalyzer = accountAnalyzer;
		this.transactionHashCache = transactionHashCache;
	}

	/**
	 * Gets the account analyzer.
	 *
	 * @return The account analyzer.
	 */
	public AccountAnalyzer getAccountAnalyzer() {
		return this.accountAnalyzer;
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
	 * returns a copy of this NIS cache.
	 *
	 * @return The copy.
	 */
	public NisCache copy() {
		return new NisCache(this.accountAnalyzer.copy(), this.transactionHashCache.shallowCopy());
	}

	/**
	 * Copies this NIS cache to the given one.
	 */
	public void shallowCopyTo(final NisCache nisCache) {
		this.accountAnalyzer.shallowCopyTo(nisCache.getAccountAnalyzer());
		this.transactionHashCache.shallowCopyTo(nisCache.getTransactionHashCache());
	}
}
