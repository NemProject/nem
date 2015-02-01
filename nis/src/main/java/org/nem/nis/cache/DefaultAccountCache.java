package org.nem.nis.cache;

import org.nem.core.crypto.*;
import org.nem.core.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A simple, in-memory account cache that implements AccountLookup and provides the lookup of accounts
 * by their addresses.
 */
public class DefaultAccountCache implements ExtendedAccountCache<DefaultAccountCache> {
	private static final Logger LOGGER = Logger.getLogger(DefaultAccountCache.class.getName());

	private final ConcurrentHashMap<Address, Account> addressToAccountMap = new ConcurrentHashMap<>();

	@Override
	public int size() {
		return this.addressToAccountMap.size();
	}

	@Override
	public CacheContents<Account> contents() {
		return new CacheContents<>(this.addressToAccountMap.values());
	}

	@Override
	public AccountCache asAutoCache() {
		return new AutoCacheDefaultAccountCache(this);
	}

	@Override
	public void shallowCopyTo(final DefaultAccountCache rhs) {
		rhs.addressToAccountMap.clear();
		rhs.addressToAccountMap.putAll(this.addressToAccountMap);
	}

	@Override
	public Account addAccountToCache(final Address address) {
		return this.findByAddress(address, () -> {
			final Account account = new Account(address);
			this.addressToAccountMap.put(address, account);
			return account;
		});
	}

	@Override
	public void removeFromCache(final Address address) {
		this.addressToAccountMap.remove(address);
	}

	private Account findByAddress(final Address address, final Supplier<Account> notFoundHandler) {
		if (!address.isValid()) {
			throw new MissingResourceException("invalid address: ", Address.class.getName(), address.toString());
		}

		final Account account = this.findByAddressImpl(address);
		return null != account ? account : notFoundHandler.get();
	}

	private Account findByAddressImpl(final Address address) {
		Account account = this.addressToAccountMap.get(address);
		if (null == account) {
			return null;
		}

		if (null == account.getAddress().getPublicKey() && null != address.getPublicKey()) {
			account = new Account(address);
			this.addressToAccountMap.put(address, account);
		}

		return account;
	}

	@Override
	public Account findByAddress(final Address address) {
		LOGGER.finer(String.format("looking for [%s] %s", address, this.size()));
		return this.findByAddress(address, () -> createAccount(address.getPublicKey(), address.getEncoded()));
	}

	private static Account createAccount(final PublicKey publicKey, final String encodedAddress) {
		return null != publicKey
				? new Account(new KeyPair(publicKey))
				: new Account(Address.fromEncoded(encodedAddress));
	}

	@Override
	public boolean isKnownAddress(final Address address) {
		return this.addressToAccountMap.containsKey(address);
	}

	@Override
	public DefaultAccountCache copy() {
		final DefaultAccountCache copy = new DefaultAccountCache();
		for (final Map.Entry<Address, Account> entry : this.addressToAccountMap.entrySet()) {
			copy.addressToAccountMap.put(entry.getKey(), entry.getValue());
		}

		return copy;
	}

	private static class AutoCacheDefaultAccountCache implements AccountCache {
		private final DefaultAccountCache accountCache;

		public AutoCacheDefaultAccountCache(final DefaultAccountCache accountCache) {
			this.accountCache = accountCache;
		}

		@Override
		public Account findByAddress(final Address id) {
			return this.accountCache.addAccountToCache(id);
		}

		@Override
		public boolean isKnownAddress(final Address address) {
			return this.accountCache.isKnownAddress(address);
		}

		@Override
		public int size() {
			return this.accountCache.size();
		}

		@Override
		public CacheContents<Account> contents() {
			return this.accountCache.contents();
		}

		@Override
		public Account addAccountToCache(final Address address) {
			return this.accountCache.addAccountToCache(address);
		}

		@Override
		public void removeFromCache(final Address address) {
			this.accountCache.removeFromCache(address);
		}
	}
}