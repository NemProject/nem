package org.nem.nis.websocket;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChain;
import org.nem.nis.harvesting.UnconfirmedState;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.AccountMetaDataFactory;
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
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;

	final Set<Address> observedAddresses;

	@Autowired
	public MessagingService(
			final BlockChain blockChain,
			final UnconfirmedState unconfirmedState,
			final SimpMessagingTemplate messagingTemplate,
			final AccountInfoFactory accountInfoFactory,
			final AccountMetaDataFactory accountMetaDataFactory,
			final UnconfirmedTransactionsFilter unconfirmedTransactions)
	{
		this.messagingTemplate = messagingTemplate;
		this.accountInfoFactory = accountInfoFactory;
		this.accountMetaDataFactory = accountMetaDataFactory;
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

		final Set<Address> changed = new HashSet<>();
		for (final Transaction transaction : block.getTransactions()) {
			pushTransaction("transactions", changed, block.getHeight(), null, transaction);
		}

		// if observed account data has changed let's push it:
		changed.stream().forEach(
				a -> this.messagingTemplate.convertAndSend("/account/" + a, this.getMetaDataPair(a))
		);
	}

	private void pushTransaction(final String prefix, final Set<Address> changed, final BlockHeight height, final Transaction parent, final Transaction transaction) {
		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER: {
				final TransferTransaction t = (TransferTransaction)transaction;
				if (this.observedAddresses.contains(t.getSigner().getAddress())) {
					if (changed != null) { changed.add(t.getSigner().getAddress()); }
					final Transaction content = parent  == null ? transaction : parent;
					this.messagingTemplate.convertAndSend(String.format("/%s/%s", prefix, t.getSigner().getAddress()),
							new TransactionMetaDataPair(content, new TransactionMetaData(height, 0L, HashUtils.calculateHash(content))));

				}
				// can't be "else if", as wee need to message it to both channels (sender and recipient)
				// TODO: probably we should check if given tx was send already, not to send same tx multiple times
				if (this.observedAddresses.contains(t.getRecipient().getAddress())) {
					if (changed != null) { changed.add(t.getRecipient().getAddress()); }
					final Transaction content = parent  == null ? transaction : parent;
					this.messagingTemplate.convertAndSend(String.format("/%s/%s", prefix, t.getRecipient().getAddress()),
							new TransactionMetaDataPair(content, new TransactionMetaData(height, 0L, HashUtils.calculateHash(content))));
				}
			}
			break;
			case TransactionTypes.MULTISIG: {
				final MultisigTransaction t = (MultisigTransaction)transaction;
				if (this.observedAddresses.contains(t.getSigner().getAddress())) {
					if (changed != null) { changed.add(t.getSigner().getAddress()); }
					this.messagingTemplate.convertAndSend(String.format("/%s/%s", prefix, t.getSigner().getAddress()),
							new TransactionMetaDataPair(t, new TransactionMetaData(height, 0L, HashUtils.calculateHash(t))));
				}
				this.pushTransaction(prefix, changed, height, t, t.getOtherTransaction());
			}
			break;
			default:
				break;
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
		this.pushTransaction("unconfirmed", null, JS_MAX_SAFE_INTEGER, null, transaction);
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
}
