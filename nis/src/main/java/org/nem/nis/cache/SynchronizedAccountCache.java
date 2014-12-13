package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.util.Iterator;

/**
 * A synchronized AccountCache implementation.
 */
public class SynchronizedAccountCache implements AccountCache, CopyableCache<SynchronizedAccountCache> {
	private final DefaultAccountCache accountCache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param accountCache The account cache.
	 */
	public SynchronizedAccountCache(final DefaultAccountCache accountCache) {
		this.accountCache = accountCache;
	}

	@Override
	public Account addAccountToCache(final Address address) {
		synchronized (this.lock) {
			return this.accountCache.addAccountToCache(address);
		}
	}

	@Override
	public void removeFromCache(final Address address) {
		synchronized (this.lock) {
			this.accountCache.removeFromCache(address);
		}
	}

	@Override
	public AccountLookup asAutoCache() {
		synchronized (this.lock) {
			return this.accountCache.asAutoCache();
		}
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.accountCache.size();
		}
	}

	@Override
	public CacheContents<Account> contents() {
		synchronized (this.lock) {
			return this.accountCache.contents();
		}
	}

	@Override
	public Account findByAddress(final Address id) {
		synchronized (this.lock) {
			return this.accountCache.findByAddress(id);
		}
	}

	@Override
	public boolean isKnownAddress(final Address id) {
		synchronized (this.lock) {
			return this.accountCache.isKnownAddress(id);
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedAccountCache rhs) {
		synchronized (this.lock) {
			this.accountCache.shallowCopyTo(rhs.accountCache);
		}
	}

	@Override
	public SynchronizedAccountCache copy() {
		synchronized (this.lock) {
			return new SynchronizedAccountCache(this.accountCache.copy());
		}
	}
}
