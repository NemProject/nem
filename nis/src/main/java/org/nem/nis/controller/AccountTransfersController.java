package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.cache.ReadOnlyHashCache;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.service.AccountIo;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountTransfersController {
	private final AccountIo accountIo;
	private final ReadOnlyHashCache transactionHashCache;
	private final NisConfiguration nisConfiguration;

	@Autowired(required = true)
	AccountTransfersController(
			final AccountIo accountIo,
			final ReadOnlyHashCache transactionHashCache,
			final NisConfiguration nisConfiguration) {
		this.accountIo = accountIo;
		this.transactionHashCache = transactionHashCache;
		this.nisConfiguration = nisConfiguration;
	}

	//region /account/transfers/*

	/**
	 * Gets information about transactions of a specified account ending at the specified transaction (via hash or id).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/all", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersAll(final AccountTransactionsPageBuilder builder) {
		return this.getAccountTransfersUsingId(builder.build(), ReadOnlyTransferDao.TransferType.ALL);
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash or id).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/incoming", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersIncoming(final AccountTransactionsPageBuilder builder) {
		return this.getAccountTransfersUsingId(builder.build(), ReadOnlyTransferDao.TransferType.INCOMING);
	}

	/**
	 * Gets information about outgoing transactions of a specified account ending at the specified transaction (via hash or id).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/outgoing", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersOutgoing(final AccountTransactionsPageBuilder builder) {
		return this.getAccountTransfersUsingId(builder.build(), ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	//endregion

	//region /local/account/transfers/*

	/**
	 * Gets information about transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key.
	 *
	 * @param page The page.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/all", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersAll(@RequestBody final AccountPrivateKeyTransactionsPage page) {
		return this.transformPairs(this.accountTransfersAll(page.createPageBuilder()), page.getPrivateKey());
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key.
	 *
	 * @param page The page.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/incoming", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersIncoming(@RequestBody final AccountPrivateKeyTransactionsPage page) {
		return this.transformPairs(this.accountTransfersIncoming(page.createPageBuilder()), page.getPrivateKey());
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key.
	 *
	 * @param page The page.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/outgoing", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersOutgoing(@RequestBody final AccountPrivateKeyTransactionsPage page) {
		return this.transformPairs(this.accountTransfersOutgoing(page.createPageBuilder()), page.getPrivateKey());
	}

	private SerializableList<TransactionMetaDataPair> transformPairs(
			final SerializableList<TransactionMetaDataPair> originalPairs,
			final PrivateKey privateKey) {
		final Collection<TransactionMetaDataPair> pairs = originalPairs.asCollection().stream()
				.map(p -> this.tryCreateDecodedPair(p, privateKey))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	private TransactionMetaDataPair tryCreateDecodedPair(final TransactionMetaDataPair pair, final PrivateKey privateKey) {
		final Transaction transaction = pair.getEntity();
		if (TransactionTypes.TRANSFER == transaction.getType()) {
			final TransferTransaction t = (TransferTransaction)transaction;
			if (null != t.getMessage() && MessageTypes.SECURE == t.getMessage().getType()) {
				final Account account = new Account(new KeyPair(privateKey));
				final SecureMessage message = t.getSigner().equals(account)
						? SecureMessage.fromEncodedPayload(account, t.getRecipient(), t.getMessage().getEncodedPayload())
						: SecureMessage.fromEncodedPayload(t.getSigner(), account, t.getMessage().getEncodedPayload());
				if (!message.canDecode()) {
					return pair;
				}

				// TODO 20150720 J-B: do we need to copy mosaics to the decoded transaction?
				// TODO 20150727 BR -> J: sure, the user wants to see the entire transaction
				final Message plainMessage = new PlainMessage(message.getDecodedPayload());
				final TransferTransaction decodedTransaction = new TransferTransaction(
						t.getTimeStamp(),
						t.getSigner(),
						t.getRecipient(),
						t.getAmount(),
						new TransferTransactionAttachment(plainMessage));
				decodedTransaction.setFee(t.getFee());
				decodedTransaction.setDeadline(t.getDeadline());
				decodedTransaction.setSignature(t.getSignature());
				return new TransactionMetaDataPair(decodedTransaction, pair.getMetaData());
			}
		}

		return pair;
	}

	private SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(
			final AccountTransactionsPage page,
			final ReadOnlyTransferDao.TransferType transferType) {
		if (null != page.getId()) {
			return this.accountIo.getAccountTransfersUsingId(page.getAddress(), page.getId(), transferType);
		}

		final Hash hash = page.getHash();
		if (null == hash) {
			// if a hash was not specified, get the latest transactions for the account
			return this.accountIo.getAccountTransfersUsingId(page.getAddress(), null, transferType);
		}

		if (!this.isTransactionHashLookupSupported()) {
			throw new UnsupportedOperationException("this node does not support transaction hash lookup");
		}

		final HashMetaData metaData = this.transactionHashCache.get(hash);
		if (null != metaData) {
			return this.accountIo.getAccountTransfersUsingHash(
					page.getAddress(),
					hash,
					metaData.getHeight(),
					transferType);
		} else {
			throw new IllegalArgumentException("Neither transaction id was supplied nor hash was found in cache");
		}
	}

	private boolean isTransactionHashLookupSupported() {
		return Arrays.stream(this.nisConfiguration.getOptionalFeatures()).anyMatch(f -> f == NodeFeature.TRANSACTION_HASH_LOOKUP);
	}

	//endregion
}