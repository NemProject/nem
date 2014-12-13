package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public class DefaultNisCache implements ReadOnlyNisCache {
	private final AccountCache accountCache;
	private final SynchronizedAccountStateCache accountStateCache;
	private final SynchronizedPoiFacade poiFacade;
	private final DefaultHashCache transactionHashCache;

	/**
	 * Creates a NIS cache from an existing account cache, a poi facade and a transaction hash cache.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @param transactionHashCache the transaction hash cache.
	 */
	public DefaultNisCache(
			final AccountCache accountCache,
			final SynchronizedAccountStateCache accountStateCache,
			final SynchronizedPoiFacade poiFacade,
			final DefaultHashCache transactionHashCache) {
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
	public ReadOnlyAccountStateCache getAccountStateCache() {
		return this.accountStateCache;
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
	public DefaultHashCache getTransactionHashCache() {
		return this.transactionHashCache;
	}

	@Override
	public NisCache copy() {
		return new DefaultNisCacheCopy(this);
	}

	private static class DefaultNisCacheCopy implements NisCache {
		private final DefaultNisCache cache;
		private final AccountCache accountCache;
		private final SynchronizedAccountStateCache accountStateCache;
		private final SynchronizedPoiFacade poiFacade;
		private final DefaultHashCache transactionHashCache;

		private DefaultNisCacheCopy(final DefaultNisCache cache) {
			this.cache = cache;
			this.accountCache = cache.accountCache.copy();
			this.accountStateCache = cache.accountStateCache.copy();
			this.poiFacade = cache.poiFacade.copy();
			this.transactionHashCache = cache.transactionHashCache.copy();
		}

		@Override
		public AccountCache getAccountCache() {
			return this.accountCache;
		}

		@Override
		public AccountStateCache getAccountStateCache() {
			return this.accountStateCache;
		}

		@Override
		public PoiFacade getPoiFacade() {
			return this.poiFacade;
		}

		@Override
		public DefaultHashCache getTransactionHashCache() {
			return this.transactionHashCache;
		}

		@Override
		public NisCache copy() {
			 throw new IllegalStateException("nested copies are not currently allowed");
		}

		@Override
		public void commit() {
			this.accountCache.shallowCopyTo(this.cache.accountCache);
			this.accountStateCache.shallowCopyTo(this.cache.accountStateCache);
			this.poiFacade.shallowCopyTo(this.cache.poiFacade);
			this.transactionHashCache.shallowCopyTo(this.cache.transactionHashCache);
		}
	}
}