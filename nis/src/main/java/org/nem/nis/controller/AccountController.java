package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.dao.ReadOnlyTransferDao;
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
	private final ReadOnlyHashCache transactionHashCache;

	@Autowired(required = true)
	AccountController(
			final UnconfirmedTransactions unconfirmedTransactions,
			final UnlockedAccounts unlockedAccounts,
			final AccountIo accountIo,
			final ReadOnlyAccountStateCache accountStateCache,
			final ReadOnlyHashCache transactionHashCache) {
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.unlockedAccounts = unlockedAccounts;
		this.accountIo = accountIo;
		this.accountStateCache = accountStateCache;
		this.transactionHashCache = transactionHashCache;
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

	/**
	 * Checks if given account is unlocked.
	 *
	 * @param address The address of the account to check.
	 */
	@RequestMapping(value = "/account/isunlocked", method = RequestMethod.POST)
	@ClientApi
	public String accountIsUnlocked(@RequestBody final Address address) {
		final Account account = new Account(address);
		// TODO 20141222 BR: I guess the attack gimre described is not that severe?
		// TODO 20141222 J-*: I didn't think it was so severe, as this can still be figured out by which machine
		// > is originating the block (but harder), and the ecosystem make nicer graphs
		// TODO 20141229 G-J: but it makes harvesting script more complicated, cause
		// > if you want turn harvesting on and check if it's actually harvesting you need both
		// > private key and address...
		return this.unlockedAccounts.isAccountUnlocked(account) ? "ok" : "nope";
	}

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

	/**
	 * Gets information about transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key. The AccountTransactionsPagePrivateKeyPair constructor
	 * already has checked that the supplied address can be derived from the private key.
	 *
	 * @param pair The pair.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/all", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersAll(@RequestBody final AccountTransactionsPagePrivateKeyPair pair) {
		return this.transformPairs(this.accountTransfersAll(pair.createPageBuilder()), pair.getPrivateKey());
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key. The AccountTransactionsPagePrivateKeyPair constructor
	 * already has checked that the supplied address can be derived from the private key.
	 *
	 * @param pair The pair.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/incoming", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersIncoming(@RequestBody final AccountTransactionsPagePrivateKeyPair pair) {
		return this.transformPairs(this.accountTransfersIncoming(pair.createPageBuilder()), pair.getPrivateKey());
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash or id).
	 * Transaction messages are decrypted with the supplied private key. The AccountTransactionsPagePrivateKeyPair constructor
	 * already has checked that the supplied address can be derived from the private key.
	 *
	 * @param pair The pair.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/local/account/transfers/outgoing", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public SerializableList<TransactionMetaDataPair> localAccountTransfersOutgoing(@RequestBody final AccountTransactionsPagePrivateKeyPair pair) {
		return this.transformPairs(this.accountTransfersOutgoing(pair.createPageBuilder()), pair.getPrivateKey());
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
		final Transaction transaction = pair.getTransaction();
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
				final Message plainMessage = new PlainMessage(message.getDecodedPayload());
				final TransferTransaction decodedTransaction = new TransferTransaction(
						t.getTimeStamp(),
						t.getSigner(),
						t.getRecipient(),
						t.getAmount(),
						plainMessage);
				decodedTransaction.setFee(t.getFee());
				decodedTransaction.setDeadline(t.getDeadline());
				decodedTransaction.setSignature(t.getSignature());
				return new TransactionMetaDataPair(decodedTransaction, pair.getMetaData());
			}
		}

		return pair;
	}

	// The GUI should never query with a hash as parameter because it is slower. When the GUI starts however it neither has an id
	// nor a hash. So we need a method which accepts only address and transfer type as parameters.
	// Not sure if we should support hash as parameter, I left it in order to allow older NCCs/GUIs to query newer NIS versions.
	// TODO 20141205 J-B: i think we should drop support for hash in release N + 1
	// TODO 20141206 BR -> J: the id of a tx can be dependent on the node (at least in principle) and cannot be calculated from transaction data while
	// > a transaction hash is independent of the node queried and can be calculated from a transaction. It seems very natural to query by hash, the
	// > only problem being the database not supporting it. My idea was to have nodes with unlimited hash cache which can accept such a query. But i don't know
	// > how a node knows if a remote supports the operation.
	// TODO 20141206 BR -> J: this goes back to my idea of having something (an enum) that is part of each Node that indicates the services it supports
	// TODO 20141206 BR -> J: > 'a transaction hash is independent of the node queried and can be calculated from a transaction' i agree for exposing something like
	// 'find transaction by hash', but assuming we have that, then supporting finding transactions near a hash would only save us one hop
	// (the two hop solution would be (hop 1) 'find transaction by hash' / get id / (hop 2) 'get account transfers using id')
	// i suppose i don't have a strong preference if you want to leave the hash-based apis
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