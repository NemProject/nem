package org.nem.nis.websocket;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.ncc.AccountMetaData;
import org.nem.core.model.ncc.AccountMetaDataPair;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChain;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.harvesting.UnconfirmedState;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.harvesting.UnlockedAccounts;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.state.RemoteStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class MessagingService implements BlockListener, UnconfirmedTransactionListener {
	private final SimpMessagingTemplate messagingTemplate;
	private final BlockChain blockChain;
	private final UnconfirmedState unconfirmedState;

	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;
	private final ReadOnlyAccountStateCache accountStateCache;

	final Set<Address> observedAddresses;

	@Autowired
	public MessagingService(
			final SimpMessagingTemplate messagingTemplate,
			final BlockChain blockChain,
			final UnconfirmedState unconfirmedState,
			final UnlockedAccounts unlockedAccounts,
			final UnconfirmedTransactionsFilter unconfirmedTransactions,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory,
			final ReadOnlyAccountStateCache accountStateCache)
	{
		this.messagingTemplate = messagingTemplate;
		this.blockChain = blockChain;
		this.unconfirmedState = unconfirmedState;
		this.unlockedAccounts = unlockedAccounts;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.accountStateCache = accountStateCache;

		this.observedAddresses = new HashSet<>();

		this.blockChain.addListener(this);
		this.unconfirmedState.addListener(this);
	}

	/* this is responsible for registering accounts that we will want to observe */
	public void registerAccount(final Address address) {
		this.observedAddresses.add(address);
	}

	public void pushBlock(final Block block) {
		this.messagingTemplate.convertAndSend("/blocks", block);

		for (final Transaction transaction : block.getTransactions()) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final TransferTransaction t = (TransferTransaction)transaction;
					if (this.observedAddresses.contains(t.getSigner().getAddress())) {
						this.messagingTemplate.convertAndSend(String.format("/transactions/%s", t.getSigner().getAddress()), t);
					} else if (this.observedAddresses.contains(t.getRecipient().getAddress())) {
						this.messagingTemplate.convertAndSend(String.format("/transactions/%s", t.getRecipient().getAddress()), t);
					}
				}
				default:
					break;
			}
		}
	}

	@Override
	public void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore) {
		peerChain.forEach(this::pushBlock);
	}

	@Override
	public void pushTransaction(final Transaction transaction, final ValidationResult validationResult) {
		this.messagingTemplate.convertAndSend("/unconfirmed", transaction);
	}

	public void pushAccount(final Address address) {
		this.messagingTemplate.convertAndSend("/account/" + address, this.getMetaDataPair(address));
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.getMetaData(address);
		return new AccountMetaDataPair(accountInfo, metaData);
	}

	private AccountMetaData getMetaData(final Address address) {
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

				default:
					throw new IllegalStateException("unexpected remote state for account with pending importance transfer");
			}
		}

		final List<AccountInfo> cosignatoryOf = accountState.getMultisigLinks().getCosignatoriesOf().stream()
				.map(this.accountInfoFactory::createInfo)
				.collect(Collectors.toList());
		final List<AccountInfo> cosignatories = accountState.getMultisigLinks().getCosignatories().stream()
				.map(this.accountInfoFactory::createInfo)
				.collect(Collectors.toList());

		return new AccountMetaData(
				this.getAccountStatus(address),
				remoteStatus,
				cosignatoryOf,
				cosignatories);
	}

	private AccountRemoteStatus getRemoteStatus(final ReadOnlyAccountState accountState, final BlockHeight height) {
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		return remoteStatus.toAccountRemoteStatus();
	}

	private boolean hasPendingImportanceTransfer(final Address address) {
		final Collection<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, Integer.MAX_VALUE);
		return transactions.stream().anyMatch(transaction -> TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType());
	}

	private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}
}
