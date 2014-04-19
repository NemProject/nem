package org.nem.nis;

import java.util.*;
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

	private final Map<PublicKey, Account> mapByPublicKey;
	private final Map<String, Account> mapByAddressId;

	/**
	 * Creates a new, empty account cache.
	 */
	public AccountAnalyzer() {
		mapByPublicKey = new HashMap<>();
		mapByAddressId = new HashMap<>();
	}

	/**
	 * Gets the public key to Account map.
	 *
	 * @return The public key to Account map.
	 */
	public Map<PublicKey, Account> getPublicKeyMap() {
		return Collections.unmodifiableMap(this.mapByPublicKey);
	}

	/**
	 * Gets the encoded address to Account map.
	 *
	 * @return The encoded address to Account map.
	 */
	public Map<String, Account> getEncodedAddressMap() {
		return Collections.unmodifiableMap(this.mapByAddressId);
	}

	public AccountAnalyzer(final AccountAnalyzer rhs) {
		this();

		for (Map.Entry<String, Account> pair : rhs.mapByAddressId.entrySet()) {
			mapByAddressId.put(pair.getKey(), new VirtualAccount(pair.getValue()));
		}

		for (Map.Entry<PublicKey, Account> pair : rhs.mapByPublicKey.entrySet()) {
			mapByPublicKey.put(pair.getKey(), new VirtualAccount(pair.getValue()));
		}
	}

	public void replace(AccountAnalyzer other) {
		synchronized (this) {
//			this.mapByAddressId = other.mapByAddressId;
//			this.mapByPublicKey = other.mapByPublicKey;
		}
	}

	/**
	 * Adds an account to the cache if it is not already in the cache
	 *
	 * @param address The account's address.
	 * @return The account.
	 */
	public Account addAccountToCache(final Address address) {
		return this.findByAddress(address, new Func<Account>() {

			@Override
			public Account evaluate() {
				return addAccountToCache(address.getPublicKey(), address.getEncoded());
			}
		});
	}

	private Account addAccountToCache(final PublicKey publicKey, final String encodedAddress) {
		final Account account = createAccount(publicKey, encodedAddress);
		if (null != publicKey) {
			this.mapByPublicKey.put(publicKey, account);
		}

		this.mapByAddressId.put(encodedAddress, account);
		return account;
	}

	private Account findByAddress(final Address address, final Func<Account> notFoundHandler) {
		if (!address.isValid()) {
			throw new MissingResourceException("invalid address: ", Address.class.getName(), address.toString());
		}

		final Account account = findByAddress(address.getPublicKey(), address.getEncoded());
		return null != account ? account : notFoundHandler.evaluate();
	}

	private Account findByAddress(final PublicKey publicKey, final String encodedAddress) {
		// if possible return by public key
		if (null != publicKey && mapByPublicKey.containsKey(publicKey)) {
			return mapByPublicKey.get(publicKey);
		}

		// otherwise try to return by address
		if (mapByAddressId.containsKey(encodedAddress)) {
			final Account oldAccount = mapByAddressId.get(encodedAddress);

			// if possible, update account's public key
			if (null != publicKey) {
				// note that if an account does not have a public key, it can only have a balance
				// so we only need to copy the balance to the new account
				final Account account = new Account(new KeyPair(publicKey));
				final Amount balance = oldAccount.getBalance();
				account.incrementBalance(balance);
				mapByAddressId.put(encodedAddress, account);

				// associate public key with an account
				mapByPublicKey.put(publicKey, account);
			}

			return mapByAddressId.get(encodedAddress);
		}

		return null;
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
		LOGGER.finer("looking for [" + address + "]" + Integer.toString(mapByAddressId.size()));

		return this.findByAddress(address, new Func<Account>() {

			@Override
			public Account evaluate() {
				return createAccount(address.getPublicKey(), address.getEncoded());
			}
		});
	}

	private static Account createAccount(final PublicKey publicKey, final String encodedAddress) {
		return null != publicKey
				? new Account(new KeyPair(publicKey))
				: new Account(Address.fromEncoded(encodedAddress));
	}

	/**
	 * Returns an AccountLookup that automatically caches unknown accounts.
	 *
	 * @return An AccountLookup that automatically caches unknown accounts.
	 */
	public AccountLookup asAutoCache() {
		return new AutoCacheAccountLookup(this);
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
