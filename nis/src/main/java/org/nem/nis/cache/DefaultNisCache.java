package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public class DefaultNisCache implements ReadOnlyNisCache {
	private final SynchronizedAccountCache accountCache;
	private final SynchronizedAccountStateCache accountStateCache;
	private final SynchronizedPoiFacade poiFacade;
	private final SynchronizedHashCache transactionHashCache;
	private final SynchronizedNamespaceCache namespaceCache;

	/**
	 * Creates a NIS cache from an existing account cache, a poi facade and a transaction hash cache.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @param poiFacade The poi facade.
	 * @param transactionHashCache The cache of transaction hashes.
	 * @param namespaceCache The namespace cache.
	 */
	public DefaultNisCache(
			final SynchronizedAccountCache accountCache,
			final SynchronizedAccountStateCache accountStateCache,
			final SynchronizedPoiFacade poiFacade,
			final SynchronizedHashCache transactionHashCache,
			final SynchronizedNamespaceCache namespaceCache) {
		this.accountCache = accountCache;
		this.accountStateCache = accountStateCache;
		this.poiFacade = poiFacade;
		this.transactionHashCache = transactionHashCache;
		this.namespaceCache = namespaceCache;
	}

	@Override
	public AccountCache getAccountCache() {
		return this.accountCache;
	}

	@Override
	public ReadOnlyAccountStateCache getAccountStateCache() {
		return this.accountStateCache;
	}

	@Override
	public ReadOnlyPoiFacade getPoiFacade() {
		return this.poiFacade;
	}

	@Override
	public ReadOnlyHashCache getTransactionHashCache() {
		return this.transactionHashCache;
	}

	@Override
	public ReadOnlyNamespaceCache getNamespaceCache() {
		return this.namespaceCache;
	}

	@Override
	public NisCache copy() {
		return new DefaultNisCacheCopy(this);
	}

	/**
	 * Creates a deep copy of this NIS cache.
	 *
	 * @return The deep copy.
	 */
	public DefaultNisCache deepCopy() {
		return new DefaultNisCache(
				this.accountCache.copy(),
				this.accountStateCache.copy(),
				this.poiFacade.copy(),
				this.transactionHashCache.copy(),
				this.namespaceCache.copy());
	}

	private static class DefaultNisCacheCopy implements NisCache {
		private final DefaultNisCache cache;
		private final SynchronizedAccountCache accountCache;
		private final SynchronizedAccountStateCache accountStateCache;
		private final SynchronizedPoiFacade poiFacade;
		private final SynchronizedHashCache transactionHashCache;
		private final SynchronizedNamespaceCache namespaceCache;

		private DefaultNisCacheCopy(final DefaultNisCache cache) {
			this.cache = cache;
			this.accountCache = cache.accountCache.copy();
			this.accountStateCache = cache.accountStateCache.copy();
			this.poiFacade = cache.poiFacade.copy();
			this.transactionHashCache = cache.transactionHashCache.copy();
			this.namespaceCache = cache.namespaceCache.copy();
		}

		@Override
		public AccountCache getAccountCache() {
			return this.accountCache.asAutoCache();
		}

		@Override
		public AccountStateCache getAccountStateCache() {
			return this.accountStateCache.asAutoCache();
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
		public NamespaceCache getNamespaceCache() {
			return this.namespaceCache;
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
			this.namespaceCache.shallowCopyTo(this.cache.namespaceCache);
		}
	}
}
