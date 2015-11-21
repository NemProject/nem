package org.nem.nis.websocket;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.ncc.*;
import org.nem.core.model.observers.ProvisionNamespaceNotification;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChain;
import org.nem.nis.harvesting.UnconfirmedState;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.AccountMetaDataFactory;
import org.nem.nis.service.MosaicInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class MessagingService implements BlockListener, UnconfirmedTransactionListener {
	public static final BlockHeight JS_MAX_SAFE_INTEGER = new BlockHeight(9007199254740991L); // 2^53 -1

	private final SimpMessagingTemplate messagingTemplate;

	private final AccountInfoFactory accountInfoFactory;
	private final AccountMetaDataFactory accountMetaDataFactory;
	private final MosaicInfoFactory mosaicInfoFactory;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;

	final Set<Address> observedAddresses;

	private static class BlockChangedAccounts {
		final Set<Address> changedAccounts = new HashSet<>();
		final Set<Address> changedAccountMosaics = new HashSet<>();

		public Collection<Address> getChangedAccounts() {
			return changedAccounts;
		}

		public void addAccount(final Address address) {
			this.changedAccounts.add(address);
		}

		public void addAccountMosaic(final Address address) {
			this.changedAccountMosaics.add(address);
		}

		public Collection<Address> getChangedAccountMosaics() {
			return changedAccountMosaics;
		}
	}


	@Autowired
	public MessagingService(
			final BlockChain blockChain,
			final UnconfirmedState unconfirmedState,
			final SimpMessagingTemplate messagingTemplate,
			final AccountInfoFactory accountInfoFactory,
			final AccountMetaDataFactory accountMetaDataFactory,
			final MosaicInfoFactory mosaicInfoFactory,
			final UnconfirmedTransactionsFilter unconfirmedTransactions)
	{
		this.messagingTemplate = messagingTemplate;
		this.accountInfoFactory = accountInfoFactory;
		this.accountMetaDataFactory = accountMetaDataFactory;
		this.mosaicInfoFactory = mosaicInfoFactory;
		this.unconfirmedTransactions = unconfirmedTransactions;

		this.observedAddresses = new HashSet<>();

		blockChain.addListener(this);
		unconfirmedState.addListener(this);
	}

	/**
	 * Registers account, which should be observed for changes in the chani
	 *
	 * @param address Accounts' address
	 */
	public void registerAccount(final Address address) {
		this.observedAddresses.add(address);
	}

	/**
	 * Pushes new block to a /block endpoint and transactions within a block involving
	 * observed acconuts into /transactions/<address> endpoint.
	 * Finally pushes accounts that could change within a block to /account/<address> endpoint.
	 *
	 * @param block Block to be pushed
	 */
	public void pushBlock(final Block block) {
		this.messagingTemplate.convertAndSend("/blocks", block);

		final BlockChangedAccounts blockChangedAccounts = new BlockChangedAccounts();
		for (final Transaction transaction : block.getTransactions()) {
			pushTransaction("transactions", blockChangedAccounts, block.getHeight(), transaction);
		}

		// if observed account data has changed let's push it:
		blockChangedAccounts.getChangedAccounts().stream().forEach(this::pushAccount);
		blockChangedAccounts.getChangedAccountMosaics().stream().forEach(this::pushOwnedMosaic);
	}


	private void pushTransaction(final String prefix, final BlockChangedAccounts blockChangedAccounts, final BlockHeight height, final Transaction transaction) {
		pushTransaction(prefix, blockChangedAccounts, height, transaction, null);
	}

	private void pushTransaction(final String prefix, final BlockChangedAccounts blockChangedAccounts, final BlockHeight height, final Transaction transaction, final TransactionMetaDataPair optionalMetaDataPair) {
		final TransactionMetaDataPair transactionMetaDataPair = transaction.getType() != TransactionTypes.MULTISIG ?
				(optionalMetaDataPair != null ? optionalMetaDataPair : new TransactionMetaDataPair(transaction, new TransactionMetaData(height, 0L, HashUtils.calculateHash(transaction))))
				: null;

		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER: {
				final TransferTransaction t = (TransferTransaction)transaction;
				pushToAddress(prefix, blockChangedAccounts, transactionMetaDataPair, t.getSigner().getAddress());
				pushToAddress(prefix, blockChangedAccounts, transactionMetaDataPair, t.getRecipient().getAddress());

				if (blockChangedAccounts != null && t.getMosaics().size() > 0) {
					if (optionalMetaDataPair != null) {
						blockChangedAccounts.addAccountMosaic(optionalMetaDataPair.getEntity().getSigner().getAddress());
					}
					blockChangedAccounts.addAccountMosaic(t.getRecipient().getAddress());
					blockChangedAccounts.addAccountMosaic(t.getSigner().getAddress());
					t.getMosaics().stream()
							.map(m -> this.mosaicInfoFactory.getMosaicDefinition(m.getMosaicId()))
							.filter(m -> m.getMosaicLevy() != null)
							.forEach(m -> blockChangedAccounts.addAccountMosaic(m.getMosaicLevy().getRecipient().getAddress()));
				}
			}
			break;
			case TransactionTypes.PROVISION_NAMESPACE: {
				final ProvisionNamespaceTransaction t = (ProvisionNamespaceTransaction)transaction;
				pushToAddress(prefix, blockChangedAccounts, transactionMetaDataPair, t.getSigner().getAddress());
				// TODO: does it make sense to push to .getRentalFeeSink too?
			}
			break;
			case TransactionTypes.MULTISIG: {
				final MultisigTransaction t = (MultisigTransaction)transaction;
				final TransactionMetaDataPair metaDataPair = new TransactionMetaDataPair(t, new TransactionMetaData(height, 0L, HashUtils.calculateHash(t), HashUtils.calculateHash(t.getOtherTransaction())));
				pushToAddress(prefix, blockChangedAccounts, metaDataPair, t.getSigner().getAddress());
				this.pushTransaction(prefix, blockChangedAccounts, height, t.getOtherTransaction(), metaDataPair);
			}
			break;
			default:
				break;
		}
	}

	private void pushToAddress(String prefix, BlockChangedAccounts blockChangedAccounts, TransactionMetaDataPair transactionMetaDataPair, Address signerAddress) {
		if (this.observedAddresses.contains(signerAddress)) {
			if (blockChangedAccounts != null) { blockChangedAccounts.addAccount(signerAddress); }
			this.messagingTemplate.convertAndSend(String.format("/%s/%s", prefix, signerAddress), transactionMetaDataPair);
		}
	}

	@Override
	public void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore) {
		peerChain.forEach(this::pushBlock);
	}

	/**
	 * Publishes unconfirmed transaction to /unconfirmed endpoint.
	 * If involved account is observed also pushes to /unconfirmed/<address> endpoint.
	 *
	 * @param transaction Unconfirmed transaction.
	 * @param validationResult Result of a validation, only successful transactions are
	 */
	@Override
	public void pushTransaction(final Transaction transaction, final ValidationResult validationResult) {
		this.messagingTemplate.convertAndSend("/unconfirmed", transaction);
		this.pushTransaction("unconfirmed", null, JS_MAX_SAFE_INTEGER, transaction);
	}

	/**
	 * Helper method that retrieves information about an account and publishes it to /account/<address> endpoint
	 *
	 * @param address Account's address
	 */
	public void pushAccount(final Address address) {
		this.messagingTemplate.convertAndSend("/account/" + address, this.getMetaDataPair(address));
	}

	/**
	 * Helper method that publishes list of TransactionMetaDataPair into /recenttransactions/<address> endpoint.
	 *
	 * @param address Account's address.
	 * @param transactions List of transactions.
	 */
	public void pushTransactions(final Address address, final SerializableList<TransactionMetaDataPair> transactions) {
		this.messagingTemplate.convertAndSend("/recenttransactions/" + address, transactions);
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.accountMetaDataFactory.createMetaData(address);
		return new AccountMetaDataPair(accountInfo, metaData);
	}

	/**
	 * Retrieves list of latest unconfirmed transactions for given account, and publishes it to /unconfirmed/<address> endpoint.
	 *
	 * @param address Account's address.
	 */
	public void pushUnconfirmed(final Address address) {
		this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, 10).stream()
				.forEach(t -> this.pushTransaction(t, ValidationResult.NEUTRAL));

	}

	// experimental and most likely subject to change
	// (currently there's no associated listener, so it's not called from anywhere)
	public void pushOwnedMosaicDefinition(final Address address) {
		this.mosaicInfoFactory.getMosaicDefinitionsMetaDataPairs(address).stream()
				.forEach(t -> this.pushMosaicDefinition(address, t));
	}

	private void pushMosaicDefinition(final Address address, final MosaicDefinitionSupplyPair mosaicDefinitionSupplyPair) {
		this.messagingTemplate.convertAndSend("/account/mosaic/owned/definition/" + address, mosaicDefinitionSupplyPair);
	}

	public void pushOwnedMosaic(final Address address) {
		this.mosaicInfoFactory.getAccountOwnedMosaics(address).stream()
				.forEach(t -> this.pushMosaic(address, t));
	}

	private void pushMosaic(final Address address, final Mosaic mosaic) {
		this.messagingTemplate.convertAndSend("/account/mosaic/owned/" + address, mosaic);
	}

	public void pushOwnedNamespace(final Address address) {
		this.mosaicInfoFactory.getAccountOwnedNamespaces(address).stream()
				.forEach(t -> this.pushNamespace(address, t));
	}

	private void pushNamespace(final Address address, final Namespace namespace) {
		this.messagingTemplate.convertAndSend("/account/namespace/owned/" + address, namespace);
	}

}
