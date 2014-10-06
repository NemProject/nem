package org.nem.nis.harvesting;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

/**
 * Manages a collection of accounts eligible for harvesting.
 */
public class UnlockedAccounts implements Iterable<Account> {
	private final ConcurrentHashSet<Account> unlocked;
	private final AccountLookup accountLookup;
	private final PoiFacade poiFacade;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final NisConfiguration nisConfiguration;

	// TODO 20141005 J-G: do we expect the configuration to be "live" (in that changes take effect without restarting NIS)?
	// > if not, i would prefer to just pass in the limit as an int
	@Autowired(required = true)
	public UnlockedAccounts(
			final AccountLookup accountLookup,
			final PoiFacade poiFacade,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final NisConfiguration nisConfiguration) {
		this.accountLookup = accountLookup;
		this.poiFacade = poiFacade;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unlocked = new ConcurrentHashSet<>();
		this.nisConfiguration = nisConfiguration;
	}

	/**
	 * Unlocks the specified account for foraging.
	 *
	 * @param account The account.
	 */
	public UnlockResult addUnlockedAccount(final Account account) {
		if (this.unlocked.size() == this.nisConfiguration.getUnlockedLimit()) {
			return UnlockResult.FAILURE_SERVER_LIMIT;
		}

		if (!this.accountLookup.isKnownAddress(account.getAddress())) {
			return UnlockResult.FAILURE_UNKNOWN_ACCOUNT;
		}

		// use the latest forwarded state so that remote harvesters that aren't active yet can be unlocked
		final BlockHeight currentHeight = new BlockHeight(this.blockChainLastBlockLayer.getLastBlockHeight());
		final PoiAccountState accountState = this.poiFacade.findLatestForwardedStateByAddress(account.getAddress());
		final PoiAccountInfo accountInfo = new PoiAccountInfo(-1, accountState, currentHeight);

		if (!accountInfo.canForage()) {
			return UnlockResult.FAILURE_FORAGING_INELIGIBLE;
		}

		this.unlocked.add(account);
		return UnlockResult.SUCCESS;
	}

	/**
	 * Removes the specified account from the list of active foraging accounts.
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
	 * Gets the number of unlocked account.
	 *
	 * @return The number of unlocked account.
	 */
	public int size() {
		return this.unlocked.size();
	}
}
