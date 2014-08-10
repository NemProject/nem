package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A simple, in-memory account cache that implements AccountLookup and provides the lookup of accounts
 * by their addresses.
 */
public class AccountCache implements AccountLookup, Iterable<Account> {
	private static final Logger LOGGER = Logger.getLogger(AccountCache.class.getName());

	private final ConcurrentHashMap<Address, Account> addressToAccountMap = new ConcurrentHashMap<>();

	/**
	 * Gets the number of accounts.
	 *
	 * @return The number of accounts.
	 */
	public int size() {
		return this.addressToAccountMap.size();
	}

	/**
	 * Returns an AccountLookup that automatically caches unknown accounts.
	 *
	 * @return An AccountLookup that automatically caches unknown accounts.
	 */
	public AccountLookup asAutoCache() {
		return new AutoCacheAccountLookup(this);
	}

	/**
	 * Copies this cache's accounts to another cache's map.
	 *
	 * @param rhs The other analyzer.
	 */
	public void shallowCopyTo(final AccountCache rhs) {
		rhs.addressToAccountMap.clear();
		rhs.addressToAccountMap.putAll(this.addressToAccountMap);
	}

	/**
	 * Adds an account to the cache if it is not already in the cache
	 *
	 * @param address The address of the account to add.
	 * @return The account.
	 */
	public Account addAccountToCache(final Address address) {
		return this.findByAddress(address, () -> {
			final Account account = new Account(address);
			this.addressToAccountMap.put(address, account);
			return account;
		});
	}

	/**
	 * Removes an account from the cache if it is in the cache.
	 *
	 * @param address The address of the account to remove.
	 */
	public void removeFromCache(final Address address) {
		this.addressToAccountMap.remove(address);
	}

	private Account findByAddress(final Address address, final Supplier<Account> notFoundHandler) {
		if (!address.isValid()) {
			throw new MissingResourceException("invalid address: ", Address.class.getName(), address.toString());
		}

		final Account account = findByAddressImpl(address);
		return null != account ? account : notFoundHandler.get();
	}

	private Account findByAddressImpl(final Address address) {
		Account account = this.addressToAccountMap.get(address);
		if (null == account)
			return null;

		if (null == account.getAddress().getPublicKey() && null != address.getPublicKey()) {
			// earlier there was new object created and data copied into it
			// this was very, VERY wrong
			account.setPublicKey(address.getPublicKey());
		}

		return account;
	}

	/**
	 * Finds an account, updating it's public key if there's a need.
	 *
	 * @param address Address of an account
	 *
	 * @return Account associated with an address or new Account if address was unknown
	 */
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

	/**
	 * Creates a copy of this analyzer.
	 *
	 * @return A copy of this analyzer.
	 */
	public AccountCache copy() {
		final AccountCache copy = new AccountCache();
		for (final Map.Entry<Address, Account> entry : this.addressToAccountMap.entrySet()) {
			copy.addressToAccountMap.put(entry.getKey(), entry.getValue().copy());
		}

		return copy;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.addressToAccountMap.values().iterator();
	}

	private static class AutoCacheAccountLookup implements AccountLookup {
		private final AccountCache accountCache;

		public AutoCacheAccountLookup(final AccountCache accountCache) {
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
	}
}
