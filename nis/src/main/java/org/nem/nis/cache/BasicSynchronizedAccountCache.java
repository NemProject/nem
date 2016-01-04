package org.nem.nis.cache;

import org.nem.core.model.*;

import java.util.function.Predicate;

/**
 * A synchronized AccountCache implementation.
 */
public class BasicSynchronizedAccountCache implements AccountCache {
	protected final Object lock;
	private final AccountCache accountCache;

	/**
	 * Creates a new account cache.
	 *
	 * @param accountCache The decorated cache.
	 */
	protected BasicSynchronizedAccountCache(final AccountCache accountCache) {
		this(new Object(), accountCache);
	}

	/**
	 * Creates a new account cache.
	 *
	 * @param lock The lock object to use.
	 * @param accountCache The decorated cache.
	 */
	private BasicSynchronizedAccountCache(final Object lock, final AccountCache accountCache) {
		this.lock = lock;
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
	public Account findByAddress(final Address id, final Predicate<Address> validator) {
		synchronized (this.lock) {
			return this.accountCache.findByAddress(id, validator);
		}
	}

	@Override
	public boolean isKnownAddress(final Address id) {
		synchronized (this.lock) {
			return this.accountCache.isKnownAddress(id);
		}
	}
}
