package org.nem.nis.cache;

/**
 * A synchronized AccountCache implementation.
 */
public class SynchronizedAccountCache extends BasicSynchronizedAccountCache implements ExtendedAccountCache<SynchronizedAccountCache> {
	private final DefaultAccountCache accountCache;

	/**
	 * Creates a new cache.
	 *
	 * @param accountCache The account cache.
	 */
	public SynchronizedAccountCache(final DefaultAccountCache accountCache) {
		super(accountCache);
		this.accountCache = accountCache;
	}

	@Override
	public AccountCache asAutoCache() {
		synchronized (this.lock) {
			return new BasicSynchronizedAccountCache(this.lock, this.accountCache.asAutoCache());
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedAccountCache rhs) {
		synchronized (rhs.lock) {
			this.accountCache.shallowCopyTo(rhs.accountCache);
		}
	}

	@Override
	public SynchronizedAccountCache copy() {
		synchronized (this.lock) {
			return new SynchronizedAccountCache(this.accountCache.copy());
		}
	}

	@Override
	public void commit() {
		synchronized (this.lock) {
			this.accountCache.commit();
		}
	}

	public SynchronizedAccountCache deepCopy() {
		synchronized (this.lock) {
			return new SynchronizedAccountCache(this.accountCache.deepCopy());
		}
	}
}
