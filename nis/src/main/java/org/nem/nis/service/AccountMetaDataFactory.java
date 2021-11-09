package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.ncc.AccountMetaData;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.harvesting.UnlockedAccounts;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.state.RemoteStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// TODO 20151124 J-G: i like this refactoring, but tests ^^
@Service
public class AccountMetaDataFactory {
	private final AccountInfoFactory accountInfoFactory;
	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final ReadOnlyAccountStateCache accountStateCache;

	@Autowired(required = true)
	public AccountMetaDataFactory(final AccountInfoFactory accountInfoFactory, final UnlockedAccounts unlockedAccounts,
			final UnconfirmedTransactionsFilter unconfirmedTransactions, final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.accountInfoFactory = accountInfoFactory;
		this.unlockedAccounts = unlockedAccounts;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountStateCache = accountStateCache;
	}

	public AccountMetaData createMetaData(final Address address) {
		final BlockHeight height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		AccountRemoteStatus remoteStatus = this.getRemoteStatus(accountState, height);
		if (this.hasPendingImportanceTransfer(address)) {
			switch (remoteStatus) {
				case INACTIVE:
					remoteStatus = AccountRemoteStatus.ACTIVATING;
					break;

				case ACTIVE:
					remoteStatus = AccountRemoteStatus.DEACTIVATING;
					break;

				default :
					throw new IllegalStateException("unexpected remote state for account with pending importance transfer");
			}
		}

		final List<AccountInfo> cosignatoryOf = accountState.getMultisigLinks().getCosignatoriesOf().stream()
				.map(this.accountInfoFactory::createInfo).collect(Collectors.toList());
		final List<AccountInfo> cosignatories = accountState.getMultisigLinks().getCosignatories().stream()
				.map(this.accountInfoFactory::createInfo).collect(Collectors.toList());

		return new AccountMetaData(this.getAccountStatus(address), remoteStatus, cosignatoryOf, cosignatories);
	}

	private AccountRemoteStatus getRemoteStatus(final ReadOnlyAccountState accountState, final BlockHeight height) {
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		return remoteStatus.toAccountRemoteStatus();
	}

	private boolean hasPendingImportanceTransfer(final Address address) {
		final Collection<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address,
				Integer.MAX_VALUE);
		return transactions.stream().anyMatch(transaction -> TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType());
	}

	private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}
}
