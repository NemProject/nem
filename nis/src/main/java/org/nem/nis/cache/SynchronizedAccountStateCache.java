package org.nem.nis.cache;

/**
 * A synchronized AccountStateCache implementation.
 */
public class SynchronizedAccountStateCache extends BasicSynchronizedAccountStateCache implements ExtendedAccountStateCache<SynchronizedAccountStateCache> {
	private final DefaultAccountStateCache accountStateCache;

	/**
	 * Creates a new cache.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public SynchronizedAccountStateCache(final DefaultAccountStateCache accountStateCache) {
		super(accountStateCache);
		this.accountStateCache = accountStateCache;
	}

	public AccountStateCache asAutoCache() {
		synchronized (this.lock) {
			return new BasicSynchronizedAccountStateCache(this.lock, this.accountStateCache.asAutoCache());
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedAccountStateCache rhs) {
		synchronized (rhs.lock) {
			this.accountStateCache.shallowCopyTo(rhs.accountStateCache);
		}
	}

	@Override
	public SynchronizedAccountStateCache copy() {
		synchronized (this.lock) {
			return new SynchronizedAccountStateCache(this.accountStateCache.copy());
		}
	}
}
