package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public class DefaultNisCache implements ReadOnlyNisCache {
	private final SynchronizedAccountCache accountCache;
	private final SynchronizedAccountStateCache accountStateCache;
	private final SynchronizedPoxFacade poxFacade;
	private final SynchronizedHashCache transactionHashCache;
	private final SynchronizedNamespaceCache namespaceCache;

	/**
	 * Creates a NIS cache from an existing account cache, a pox facade and a transaction hash cache.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @param poxFacade The pox facade.
	 * @param transactionHashCache The cache of transaction hashes.
	 * @param namespaceCache The namespace cache.
	 */
	public DefaultNisCache(final SynchronizedAccountCache accountCache, final SynchronizedAccountStateCache accountStateCache,
			final SynchronizedPoxFacade poxFacade, final SynchronizedHashCache transactionHashCache,
			final SynchronizedNamespaceCache namespaceCache) {
		this.accountCache = accountCache;
		this.accountStateCache = accountStateCache;
		this.poxFacade = poxFacade;
		this.transactionHashCache = transactionHashCache;
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ReadOnlyAccountCache getAccountCache() {
		return this.accountCache;
	}

	@Override
	public ReadOnlyAccountStateCache getAccountStateCache() {
		return this.accountStateCache;
	}

	@Override
	public ReadOnlyPoxFacade getPoxFacade() {
		return this.poxFacade;
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
		return new DefaultNisCache(this.accountCache.deepCopy(), this.accountStateCache.deepCopy(), this.poxFacade.copy(),
				this.transactionHashCache.deepCopy(), this.namespaceCache.deepCopy());
	}

	private static class DefaultNisCacheCopy implements NisCache {
		private final DefaultNisCache cache;
		private final SynchronizedAccountCache accountCache;
		private final SynchronizedAccountStateCache accountStateCache;
		private final SynchronizedPoxFacade poxFacade;
		private final SynchronizedHashCache transactionHashCache;
		private final SynchronizedNamespaceCache namespaceCache;

		private DefaultNisCacheCopy(final DefaultNisCache cache) {
			this.cache = cache;
			this.accountCache = cache.accountCache.copy();
			this.accountStateCache = cache.accountStateCache.copy();
			this.poxFacade = cache.poxFacade.copy();
			this.transactionHashCache = cache.transactionHashCache.copy();
			this.namespaceCache = cache.namespaceCache.copy();
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
		public PoxFacade getPoxFacade() {
			return this.poxFacade;
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
			this.accountCache.commit();
			this.accountStateCache.commit();
			this.poxFacade.shallowCopyTo(this.cache.poxFacade);
			this.transactionHashCache.commit();
			this.namespaceCache.commit();
		}
	}
}
