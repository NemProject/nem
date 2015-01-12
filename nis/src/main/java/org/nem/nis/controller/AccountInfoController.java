package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.*;
import org.nem.nis.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for retrieving account related information
 */
@RestController
public class AccountInfoController {
	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;
	private final ReadOnlyAccountStateCache accountStateCache;

	@Autowired(required = true)
	AccountInfoController(
			final UnlockedAccounts unlockedAccounts,
			final UnconfirmedTransactions unconfirmedTransactions,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.unlockedAccounts = unlockedAccounts;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Gets information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGet(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.getMetaDataPair(address);
	}

	/**
	 * Gets a list of account information.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of account information.
	 */
	@RequestMapping(value = "/account/get/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<AccountMetaDataPair> accountGetBatch(@RequestBody final Deserializer deserializer) {
		final SerializableList<AccountId> accounts = new SerializableList<>(deserializer, AccountId::new);
		final Collection<AccountMetaDataPair> pairs = accounts.asCollection().stream()
				.map(a -> this.getMetaDataPair(a.getAddress()))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaData accountStatus(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.getMetaData(address);
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo account = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.getMetaData(address);
		return new AccountMetaDataPair(account, metaData);
	}

	private AccountMetaData getMetaData(final Address address) {
		final Long height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		AccountRemoteStatus remoteStatus = this.getRemoteStatus(accountState, new BlockHeight(height));
		if (this.hasPendingImportanceTransfer(address)) {
			switch (remoteStatus) {
				case INACTIVE:
					remoteStatus = AccountRemoteStatus.ACTIVATING;
					break;

				case ACTIVE:
					remoteStatus = AccountRemoteStatus.DEACTIVATING;
					break;

				default:
					throw new IllegalStateException("unexpected remote state for account with pending importance transfer");
			}
		}

		// TODO 20150111 J-G: should add a test for this
		final List<AccountInfo> cosignatoryOf = accountState.getMultisigLinks().getCosignatoryOf().stream()
				.map(multisigAddress -> this.accountInfoFactory.createInfo(multisigAddress))
				.collect(Collectors.toList());
		return new AccountMetaData(this.getAccountStatus(address), remoteStatus, cosignatoryOf);
	}

	private AccountRemoteStatus getRemoteStatus(final ReadOnlyAccountState accountState, final BlockHeight height) {
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		return remoteStatus.toAccountRemoteStatus();
	}

	private boolean hasPendingImportanceTransfer(final Address address) {
		final List<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, Integer.MAX_VALUE);
		for (final Transaction transaction : transactions) {
			if (TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType()) {
				return true;
			}
		}

		return false;
	}

	private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}
}
