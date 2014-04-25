package org.nem.nis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.utils.Func;

/**
 * Account cache that implements AccountLookup and provides the lookup of accounts
 * by their addresses.
 */
public class AccountAnalyzer implements AccountLookup {

	private static final Logger LOGGER = Logger.getLogger(AccountAnalyzer.class.getName());

	private final ConcurrentHashMap<Address, Account> addressToAccountMap;

	/**
	 * Creates a new, empty account cache.
	 */
	public AccountAnalyzer() {
		this.addressToAccountMap = new ConcurrentHashMap<>();
	}

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
	 * Copies this analyzer's account to address map to another analyzer's map.
	 *
	 * @param rhs The other analyzer.
	 */
	public void shallowCopyTo(final AccountAnalyzer rhs) {
		rhs.addressToAccountMap.clear();
		rhs.addressToAccountMap.putAll(this.addressToAccountMap);
	}

	/**
	 * Adds an account to the cache if it is not already in the cache
	 *
	 * @param address The account's address.
	 * @return The account.
	 */
	public Account addAccountToCache(final Address address) {
		return this.findByAddress(address, () -> {
			final Account account = new Account(address);
			addressToAccountMap.put(address, account);
			return account;
		});
	}

	private Account findByAddress(final Address address, final Func<Account> notFoundHandler) {
		if (!address.isValid()) {
			throw new MissingResourceException("invalid address: ", Address.class.getName(), address.toString());
		}

		final Account account = findByAddressImpl(address);
		return null != account ? account : notFoundHandler.evaluate();
	}

	private Account findByAddressImpl(final Address address) {
		Account account = this.addressToAccountMap.get(address);
		if (null == account)
			return null;

		if (null == account.getAddress().getPublicKey() && null != address.getPublicKey()) {
			// note that if an account does not have a public key, it can only have a balance
			// so we only need to copy the balance to the new account
			final Amount originalBalance = account.getBalance();
			account = new Account(address);
			account.incrementBalance(originalBalance);
			this.addressToAccountMap.put(address, account);
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
		LOGGER.finer("looking for [" + address + "]" + Integer.toString(addressToAccountMap.size()));

		return this.findByAddress(address, () -> createAccount(address.getPublicKey(), address.getEncoded()));
	}

	private static Account createAccount(final PublicKey publicKey, final String encodedAddress) {
		return null != publicKey
				? new Account(new KeyPair(publicKey))
				: new Account(Address.fromEncoded(encodedAddress));
	}

	/**
	 * Creates a copy of this analyzer.
	 *
	 * @return A copy of this analyzer.
	 */
	public AccountAnalyzer copy() {
		final AccountAnalyzer copy = new AccountAnalyzer();
		for (final Map.Entry<Address, Account> entry : this.addressToAccountMap.entrySet()) {
			copy.addressToAccountMap.put(entry.getKey(), entry.getValue().copy());
		}

		return copy;
	}

	private static class AutoCacheAccountLookup implements AccountLookup {

		final AccountAnalyzer accountAnalyzer;

		public AutoCacheAccountLookup(final AccountAnalyzer accountAnalyzer) {
			this.accountAnalyzer = accountAnalyzer;
		}

		@Override
		public Account findByAddress(final Address id) {
			return this.accountAnalyzer.addAccountToCache(id);
		}
	}
}
