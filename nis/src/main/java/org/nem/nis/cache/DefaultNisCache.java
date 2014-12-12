package org.nem.nis.cache;

import org.nem.nis.state.ReadOnlyAccountState;

/**
 * The central point for accessing NIS-mutable data.
 */
public class DefaultNisCache implements ReadOnlyNisCache {
	private final AccountCache accountCache;
	private final AccountStateRepository accountStateCache;
	private final SynchronizedPoiFacade poiFacade;
	private final HashCache transactionHashCache;

	/**
	 * Creates a NIS cache from an existing account cache, a poi facade and a transaction hash cache.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @param transactionHashCache the transaction hash cache.
	 */
	public DefaultNisCache(
			final AccountCache accountCache,
			final AccountStateRepository accountStateCache,
			final SynchronizedPoiFacade poiFacade,
			final HashCache transactionHashCache) {
		this.accountCache = accountCache;
		this.accountStateCache = accountStateCache;
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

	@Override
	public ReadOnlyAccountStateRepository getAccountStateCache() {
		return null;
	}

	/**
	 * Gets the poi facade.
	 *
	 * @return The poi facade.
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

	@Override
	public NisCache copy() {
		return new DefaultNisCacheCopy(this);
	}

	private static class DefaultNisCacheCopy implements NisCache {
		private final DefaultNisCache cache;
		private final AccountCache accountCache;
		private final AccountStateRepository accountStateCache;
		private final SynchronizedPoiFacade poiFacade;
		private final HashCache transactionHashCache;

		private DefaultNisCacheCopy(final DefaultNisCache cache) {
			this.cache = cache;
			// TODO 20141212 huge bug
			this.accountCache = cache.accountCache;//.copy();
			this.accountStateCache = cache.accountStateCache;
			this.poiFacade = cache.poiFacade;//.copy();
			this.transactionHashCache = cache.transactionHashCache;//.copy();
		}

		@Override
		public AccountCache getAccountCache() {
			return this.accountCache;
		}

		@Override
		public AccountStateRepository getAccountStateCache() {
			return this.accountStateCache;
		}

		@Override
		public PoiFacade getPoiFacade() {
			return this.poiFacade;
		}

		@Override
		public HashCache getTransactionHashCache() {
			return this.transactionHashCache;
		}

		@Override
		public NisCache copy() {
			return null;
		}

		@Override
		public void commit() {
			this.accountCache.shallowCopyTo(this.cache.accountCache);
			this.poiFacade.shallowCopyTo(this.cache.poiFacade);
			this.transactionHashCache.shallowCopyTo(this.cache.transactionHashCache);
		}
	}
}