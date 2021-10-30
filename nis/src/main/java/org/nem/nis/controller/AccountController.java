package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.AccountIo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {
	private static final int MAX_UNCONFIRMED_TRANSACTIONS = 25;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;
	private final UnlockedAccounts unlockedAccounts;
	private final AccountIo accountIo;
	private final ReadOnlyAccountStateCache accountStateCache;

	@Autowired(required = true)
	AccountController(final UnconfirmedTransactionsFilter unconfirmedTransactions, final UnlockedAccounts unlockedAccounts,
			final AccountIo accountIo, final ReadOnlyAccountStateCache accountStateCache) {
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
	public void accountUnlock(@Valid @RequestBody final PrivateKey privateKey) {
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
	public void accountLock(@Valid @RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.unlockedAccounts.removeUnlockedAccount(account);
	}

	// region [/local]/account/isunlocked

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
	public String isAccountUnlocked(@Valid @RequestBody final PrivateKey privateKey) {
		return this.isAccountUnlocked(Address.fromPublicKey(new KeyPair(privateKey).getPublicKey()));
	}

	// endregion

	// region unlocked/info

	/**
	 * Gets information about the unlocked accounts.
	 *
	 * @return The unlocked accounts information.
	 */
	@RequestMapping(value = "/account/unlocked/info", method = RequestMethod.POST)
	@ClientApi
	public SerializableEntity unlockedInfo() {
		return serializer -> {
			serializer.writeInt("num-unlocked", this.unlockedAccounts.size());
			serializer.writeInt("max-unlocked", this.unlockedAccounts.maxSize());
		};
	}

	// endregion

	/**
	 * Gets unconfirmed transaction information for the specified account.
	 *
	 * @param builder The account id builder.
	 * @return Information about matching transactions
	 */
	@RequestMapping(value = "/account/unconfirmedTransactions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<UnconfirmedTransactionMetaDataPair> transactionsUnconfirmed(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		final Collection<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address,
				MAX_UNCONFIRMED_TRANSACTIONS);
		final Collection<UnconfirmedTransactionMetaDataPair> pairs = transactions.stream().map(t -> {
			if (TransactionTypes.MULTISIG == t.getType()) {
				final MultisigTransaction multisig = (MultisigTransaction) t;
				return new UnconfirmedTransactionMetaDataPair(t, new UnconfirmedTransactionMetaData(multisig.getOtherTransactionHash()));
			} else {
				return new UnconfirmedTransactionMetaDataPair(t, new UnconfirmedTransactionMetaData((Hash) null));
			}
		}).collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	/**
	 * Gets information about harvested blocks.
	 *
	 * @param idBuilder The id builder.
	 * @param pageBuilder The page builder.
	 * @return Information about harvested blocks.
	 */
	@RequestMapping(value = "/account/harvests", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<HarvestInfo> accountHarvests(final AccountIdBuilder idBuilder, final DefaultPageBuilder pageBuilder) {
		final Address address = idBuilder.build().getAddress();
		final DefaultPage page = pageBuilder.build();
		return this.accountIo.getAccountHarvests(address, page.getId(), page.getPageSize());
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
				.map(a -> new AccountImportanceViewModel(a.getAddress(), a.getImportanceInfo())).collect(Collectors.toList());

		return new SerializableList<>(viewModels);
	}

	/**
	 * API for creating new account data.
	 *
	 * @return A key pair view model.
	 */
	@RequestMapping(value = "/account/generate", method = RequestMethod.GET)
	@TrustedApi
	public KeyPairViewModel generateAccount() {
		final NetworkInfo networkInfo = NetworkInfos.getDefault();
		final KeyPair keyPair = new KeyPair();
		return new KeyPairViewModel(keyPair, networkInfo.getVersion());
	}
}
