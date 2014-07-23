package org.nem.nis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.poi.*;

/**
 * Account cache that implements AccountLookup and provides the lookup of accounts
 * by their addresses.
 */
public class AccountAnalyzer implements AccountLookup, Iterable<Account> {

	private static final Logger LOGGER = Logger.getLogger(AccountAnalyzer.class.getName());

	private final ConcurrentHashMap<Address, Account> addressToAccountMap;
	private final PoiImportanceGenerator importanceGenerator;

	private BlockHeight lastPoiRecalc;

	/**
	 * Creates a new, empty account cache.
	 *
	 * @param importanceGenerator The importance generator to use.
	 */
	public AccountAnalyzer(final PoiImportanceGenerator importanceGenerator) {
		this.addressToAccountMap = new ConcurrentHashMap<>();
		this.importanceGenerator = importanceGenerator;
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
	 * Forces the account analyzer to do a poi calculation the next time recalculateImportances() is called.
	 */
	public void resetLastPoiRecalc() {
		this.lastPoiRecalc = null;
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
		rhs.lastPoiRecalc = this.lastPoiRecalc;
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
	public void removeAccountFromCache(final Address address) {
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
			account._setPublicKey(address);
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

	@Override
	public boolean isKnownAddress(final Address address) {
		return this.addressToAccountMap.containsKey(address);
	}
	
	/**
	 * Creates a copy of this analyzer.
	 *
	 * @return A copy of this analyzer.
	 */
	public AccountAnalyzer copy() {
		final AccountAnalyzer copy = new AccountAnalyzer(this.importanceGenerator);
		for (final Map.Entry<Address, Account> entry : this.addressToAccountMap.entrySet()) {
			copy.addressToAccountMap.put(entry.getKey(), entry.getValue().copy());
		}

		copy.lastPoiRecalc = this.lastPoiRecalc;
		return copy;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.addressToAccountMap.values().iterator();
	}

	/**
	 * Gets all accounts that should be included in the importance calculation
	 * at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The accounts.
	 */
	public Collection<Account> getAccounts(final BlockHeight blockHeight) {
		return this.addressToAccountMap.values().stream()
				.filter(a -> shouldIncludeInImportanceCalculation(a, blockHeight))
				.collect(Collectors.toList());
	}

	private static boolean shouldIncludeInImportanceCalculation(final Account account, final BlockHeight blockHeight) {
		return null != account.getHeight()
				&& account.getHeight().compareTo(blockHeight) <= 0
				&& !account.getAddress().equals(NemesisBlock.ADDRESS);
	}

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 */
	public void recalculateImportances(final BlockHeight blockHeight) {
		if (null != this.lastPoiRecalc && 0 == this.lastPoiRecalc.compareTo(blockHeight))
			return;

		final Collection<Account> accounts = this.getAccounts(blockHeight);
		this.importanceGenerator.updateAccountImportances(blockHeight, accounts);
		this.lastPoiRecalc = blockHeight;
	}

	/**
	 * Undo weighted balances vesting to a given block height.
	 * 
	 * @param height The block height.
	 */
	public void undoVesting(final BlockHeight height) {
		this.addressToAccountMap.values().stream().forEach(a -> a.getWeightedBalances().undoChain(height));
	}

	private static class AutoCacheAccountLookup implements AccountLookup {

		private final AccountAnalyzer accountAnalyzer;

		public AutoCacheAccountLookup(final AccountAnalyzer accountAnalyzer) {
			this.accountAnalyzer = accountAnalyzer;
		}

		@Override
		public Account findByAddress(final Address id) {
			return this.accountAnalyzer.addAccountToCache(id);
		}

		@Override
		public boolean isKnownAddress(final Address address) {
			return this.accountAnalyzer.isKnownAddress(address);
		}
	}
}
