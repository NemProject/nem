package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.AccountIdBuilder;
import org.nem.nis.harvesting.*;
import org.nem.nis.state.*;
import org.nem.nis.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for retrieving account related information
 */
@RestController
public class AccountInfoController {
	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;
	private final AccoutStateRepository accoutStateRepository;

	@Autowired(required = true)
	AccountInfoController(
			final UnlockedAccounts unlockedAccounts,
			final UnconfirmedTransactions unconfirmedTransactions,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory,
			final AccoutStateRepository accoutStateRepository) {
		this.unlockedAccounts = unlockedAccounts;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.accoutStateRepository = accoutStateRepository;
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
		final org.nem.core.model.ncc.AccountInfo account = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.accountStatus(builder);
		return new AccountMetaDataPair(account, metaData);
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaData accountStatus(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		final Long height = this.blockChainLastBlockLayer.getLastBlockHeight();
		AccountRemoteStatus remoteStatus = this.getRemoteStatus(address, new BlockHeight(height));
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

		return new AccountMetaData(this.getAccountStatus(address), remoteStatus);
	}

	private AccountRemoteStatus getRemoteStatus(final Address address, final BlockHeight height) {
		final AccountState accountState = this.accoutStateRepository.findStateByAddress(address);
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		return remoteStatus.toAccountRemoteStatus();
	}

	private boolean hasPendingImportanceTransfer(final Address address) {
		final UnconfirmedTransactions transactions = this.unconfirmedTransactions.getTransactionsForAccount(address);
		for (final Transaction transaction : transactions.getAll()) {
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
