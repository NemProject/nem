package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

/**
 * A synchronized AccountStateCache implementation.
 */
public class SynchronizedAccountStateCache implements AccountStateCache, CopyableCache<SynchronizedAccountStateCache> {
	private final DefaultAccountStateCache accountStateCache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public SynchronizedAccountStateCache(final DefaultAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public AccountState findStateByAddress(final Address address) {
		synchronized (this.lock) {
			return this.accountStateCache.findStateByAddress(address);
		}
	}

	@Override
	public AccountState findLatestForwardedStateByAddress(final Address address) {
		synchronized (this.lock) {
			return this.accountStateCache.findLatestForwardedStateByAddress(address);
		}
	}

	@Override
	public AccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
		synchronized (this.lock) {
			return this.accountStateCache.findForwardedStateByAddress(address, height);
		}
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.accountStateCache.size();
		}
	}

	@Override
	public CacheContents<ReadOnlyAccountState> contents() {
		synchronized (this.lock) {
			return this.accountStateCache.contents();
		}
	}

	@Override
	public void removeFromCache(final Address address) {
		synchronized (this.lock) {
			this.accountStateCache.removeFromCache(address);
		}
	}

	@Override
	public void undoVesting(final BlockHeight height) {
		synchronized (this.lock) {
			this.accountStateCache.undoVesting(height);
		}
	}

	@Override
	public CacheContents<AccountState> mutableContents() {
		synchronized (this.lock) {
			return this.accountStateCache.mutableContents();
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
