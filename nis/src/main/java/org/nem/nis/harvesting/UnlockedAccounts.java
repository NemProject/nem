package org.nem.nis.harvesting;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

/**
 * Manages a collection of accounts eligible for harvesting.
 */
public class UnlockedAccounts implements Iterable<Account> {
	private final AccountLookup accountLookup;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final CanHarvestPredicate canHarvestPredicate;
	private final int maxUnlockedAccounts;
	private final ConcurrentHashSet<Account> unlocked;

	@Autowired(required = true)
	public UnlockedAccounts(
			final AccountLookup accountLookup,
			final ReadOnlyAccountStateCache accountStateCache,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final CanHarvestPredicate canHarvestPredicate,
			final int maxUnlockedAccounts) {
		this.accountLookup = accountLookup;
		this.accountStateCache = accountStateCache;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.canHarvestPredicate = canHarvestPredicate;
		this.maxUnlockedAccounts = maxUnlockedAccounts;
		this.unlocked = new ConcurrentHashSet<>();
	}

	/**
	 * Unlocks the specified account for harvesting.
	 *
	 * @param account The account.
	 * @return The unlocking result.
	 */
	public UnlockResult addUnlockedAccount(final Account account) {
		if (this.unlocked.size() == this.maxUnlockedAccounts) {
			return UnlockResult.FAILURE_SERVER_LIMIT;
		}

		if (BlockedHarvesterPublicKeys.contains(account.getAddress().getPublicKey())) {
			return UnlockResult.FAILURE_HARVESTING_BLOCKED;
		}

		if (!this.accountLookup.isKnownAddress(account.getAddress())) {
			return UnlockResult.FAILURE_UNKNOWN_ACCOUNT;
		}

		// use the latest forwarded state so that remote harvesters that aren't active yet can be unlocked
		final BlockHeight currentHeight = this.blockChainLastBlockLayer.getLastBlockHeight();
		final ReadOnlyAccountState accountState = this.accountStateCache.findLatestForwardedStateByAddress(account.getAddress());
		if (!this.canHarvestPredicate.canHarvest(accountState, currentHeight)) {
			return UnlockResult.FAILURE_HARVESTING_INELIGIBLE;
		}

		this.unlocked.add(account);
		return UnlockResult.SUCCESS;
	}

	/**
	 * Removes the specified account from the list of active harvesting accounts.
	 *
	 * @param account The account.
	 */
	public void removeUnlockedAccount(final Account account) {
		if (this.accountLookup.isKnownAddress(account.getAddress())) {
			this.unlocked.remove(account);
		}
	}

	/**
	 * Determines if a given account is unlocked.
	 *
	 * @param account The account.
	 * @return true if the account is unlocked, false otherwise.
	 */
	public boolean isAccountUnlocked(final Account account) {
		return this.unlocked.contains(account);
	}

	/**
	 * Determines if a given account is unlocked.
	 *
	 * @param address The account address.
	 * @return true if the account is unlocked, false otherwise.
	 */
	public boolean isAccountUnlocked(final Address address) {
		for (final Account account : this.unlocked) {
			if (account.getAddress().equals(address)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.unlocked.iterator();
	}

	/**
	 * Gets the number of unlocked accounts.
	 *
	 * @return The number of unlocked accounts.
	 */
	public int size() {
		return this.unlocked.size();
	}

	/**
	 * Gets the maximum number of unlocked accounts.
	 *
	 * @return The maximum number of unlocked accounts.
	 */
	public int maxSize() {
		return this.maxUnlockedAccounts;
	}
}
