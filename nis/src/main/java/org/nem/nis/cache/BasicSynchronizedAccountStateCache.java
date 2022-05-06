package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

/**
 * A synchronized AccountStateCache implementation.
 */
public class BasicSynchronizedAccountStateCache implements AccountStateCache {
	protected final Object lock;
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new account cache.
	 *
	 * @param accountStateCache The decorated cache.
	 */
	protected BasicSynchronizedAccountStateCache(final AccountStateCache accountStateCache) {
		this(new Object(), accountStateCache);
	}

	/**
	 * Creates a new account cache.
	 *
	 * @param lock The lock object to use.
	 * @param accountStateCache The decorated cache.
	 */
	private BasicSynchronizedAccountStateCache(final Object lock, final AccountStateCache accountStateCache) {
		this.lock = lock;
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
}
