package org.nem.nis.websocket;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChain;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.harvesting.UnconfirmedState;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.harvesting.UnlockedAccounts;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.AccountMetaDataFactory;
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

	private final AccountInfoFactory accountInfoFactory;
	private final AccountMetaDataFactory accountMetaDataFactory;

	final Set<Address> observedAddresses;

	@Autowired
	public MessagingService(
			final SimpMessagingTemplate messagingTemplate,
			final BlockChain blockChain,
			final UnconfirmedState unconfirmedState,
			final AccountInfoFactory accountInfoFactory,
			final AccountMetaDataFactory accountMetaDataFactory)
	{
		this.messagingTemplate = messagingTemplate;
		this.blockChain = blockChain;
		this.unconfirmedState = unconfirmedState;
		this.accountInfoFactory = accountInfoFactory;
		this.accountMetaDataFactory = accountMetaDataFactory;

		this.observedAddresses = new HashSet<>();

		this.blockChain.addListener(this);
		this.unconfirmedState.addListener(this);
	}

	/* this is responsible for registering accounts that we will want to observe */
	public void registerAccount(final Address address) {
		this.observedAddresses.add(address);
		//System.out.println(String.format("REGISTERED address for observations: %s", address));
	}

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

	@Override
	public void pushTransaction(final Transaction transaction, final ValidationResult validationResult) {
		this.messagingTemplate.convertAndSend("/unconfirmed", transaction);
		this.pushTransaction("unconfirmed", null, BlockHeight.MAX, null, transaction);
	}

	public void pushAccount(final Address address) {
		this.messagingTemplate.convertAndSend("/account/" + address, this.getMetaDataPair(address));
	}

	public void pushTransactions(final Address address, final SerializableList<TransactionMetaDataPair> transactions)
	{
		this.messagingTemplate.convertAndSend("/recenttransactions/" + address, transactions);
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.accountMetaDataFactory.createMetaData(address);
		return new AccountMetaDataPair(accountInfo, metaData);
	}
}
