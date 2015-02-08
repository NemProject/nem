package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.AccountIo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {
	private static final int MAX_UNCONFIRMED_TRANSACTIONS = 25;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final UnlockedAccounts unlockedAccounts;
	private final AccountIo accountIo;
	private final ReadOnlyAccountStateCache accountStateCache;

	@Autowired(required = true)
	AccountController(
			final UnconfirmedTransactions unconfirmedTransactions,
			final UnlockedAccounts unlockedAccounts,
			final AccountIo accountIo,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.unlockedAccounts = unlockedAccounts;
		this.accountIo = accountIo;
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Unlocks an account for harvesting.
	 *
	 * @param privateKey The private key of the account to unlock.
	 */
	@RequestMapping(value = "/account/unlock", method = RequestMethod.POST)
	@ClientApi
	// TODO 20141010 J-G i think it still makes sense to reject if remote AND the private key is NOT for a remote account
	// TODO 20141010 J-G actually, i don't think this api is good enough as-is ... in its current form, i can "borrow"
	// > any nis for my harvesting purposes ... i think we need a ticket / token to allow a NIS to reject unauthorized harvesters
	// TODO 20141214 G-J: I think comment above can already be removed...
	public void accountUnlock(@RequestBody final PrivateKey privateKey) {
		final KeyPair keyPair = new KeyPair(privateKey);
		final Account account = new Account(keyPair);
		final UnlockResult result = this.unlockedAccounts.addUnlockedAccount(account);

		if (UnlockResult.SUCCESS != result) {
			throw new IllegalArgumentException(result.toString());
		}
	}

	/**
	 * Locks an account from harvesting.
	 *
	 * @param privateKey The private key of the account to lock.
	 */
	@RequestMapping(value = "/account/lock", method = RequestMethod.POST)
	@ClientApi
	public void accountLock(@RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.unlockedAccounts.removeUnlockedAccount(account);
	}

	//region [/local]/account/isunlocked

	/**
	 * Checks if the given account is unlocked.
	 *
	 * @param address The address of the account to check.
	 * @return "ok" if account is unlocked.
	 */
	@RequestMapping(value = "/account/isunlocked", method = RequestMethod.POST)
	@ClientApi
	public String isAccountUnlocked(@RequestBody final Address address) {
		final Account account = new Account(address);
		return this.unlockedAccounts.isAccountUnlocked(account) ? "ok" : "nope";
	}

	/**
	 * Checks if the given account is unlocked.
	 *
	 * @param privateKey The private key of the account to check.
	 * @return "ok" if account is unlocked.
	 */
	@RequestMapping(value = "/local/account/isunlocked", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public String isAccountUnlocked(@RequestBody final PrivateKey privateKey) {
		return this.isAccountUnlocked(Address.fromPublicKey(new KeyPair(privateKey).getPublicKey()));
	}

	//endregion

	/**
	 * Gets unconfirmed transaction information for the specified account.
	 *
	 * @param builder The account id builder.
	 * @return Information about matching transactions
	 */
	@RequestMapping(value = "/account/unconfirmedTransactions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Transaction> transactionsUnconfirmed(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		final Collection<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(
				address,
				MAX_UNCONFIRMED_TRANSACTIONS);
		return new SerializableList<>(transactions);
	}

	/**
	 * Gets information about harvested blocks.
	 *
	 * @param builder The page builder.
	 * @return information about harvested blocks
	 */
	@RequestMapping(value = "/account/harvests", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<HarvestInfo> accountHarvests(final AccountTransactionsPageBuilder builder) {
		final AccountTransactionsPage page = builder.build();
		return this.accountIo.getAccountHarvests(page.getAddress(), page.getHash());
	}

	/**
	 * Gets the current account importance information for all accounts.
	 *
	 * @return Account importance information.
	 */
	@RequestMapping(value = "/account/importances", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<AccountImportanceViewModel> getImportances() {
		final List<AccountImportanceViewModel> viewModels = this.accountStateCache.contents().stream()
				.map(a -> new AccountImportanceViewModel(a.getAddress(), a.getImportanceInfo()))
				.collect(Collectors.toList());

		return new SerializableList<>(viewModels);
	}
}