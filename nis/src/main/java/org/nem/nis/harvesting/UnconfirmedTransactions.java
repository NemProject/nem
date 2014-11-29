package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactions.class.getName());

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Hash, Boolean> pendingTransactions = new ConcurrentHashMap<>();
	private final UnconfirmedBalancesObserver unconfirmedBalances = new UnconfirmedBalancesObserver();
	private final TransactionObserver transferObserver = new TransferObserverToTransactionObserverAdapter(this.unconfirmedBalances);
	private final TransactionValidatorFactory validatorFactory;
	private final SingleTransactionValidator singleValidator;
	private final PoiFacade poiFacade;
	private final HashCache transactionHashCache;

	private enum BalanceValidationOptions {
		/**
		 * The confirmed balance check occurs as part of the single transaction validator.
		 * This is used to exclude conflicting transactions when generating a block.
		 * This is accomplished by bypassing the execution of the UnconfirmedBalancesObserver
		 * (so that all balance validations are against the current account balances).
		 */
		ValidateAgainstConfirmedBalance,

		/**
		 * The unconfirmed balance check occurs as part of the execution of the UnconfirmedBalancesObserver.
		 * This is the default setting and is used when adding new transactions and
		 * when getting the unconfirmed transactions for an account.
		 * <br/>
		 * This improves the user experience:
		 * A user complained that if the GUI shows a balance of 1k NEM he can initiate many
		 * transactions with 800 NEM. All transactions were displayed in the GUI as unconfirmed giving the user the feeling he
		 * can spend more than he has. Furthermore, a new block included one of the transactions leaving the
		 * the other transaction still being displayed as unconfirmed in the GUI forever (until deadline was exceeded).
		 */
		ValidateAgainstUnconfirmedBalance,
	}

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validatorFactory The transaction validator factory to use.
	 * @param poiFacade The poi facade to use.
	 */
	public UnconfirmedTransactions(
			final TransactionValidatorFactory validatorFactory,
			final PoiFacade poiFacade,
			final HashCache transactionHashCache) {
		this.validatorFactory = validatorFactory;
		this.poiFacade = poiFacade;
		this.transactionHashCache = transactionHashCache;
		this.singleValidator = this.createSingleValidator();
	}

	private UnconfirmedTransactions(
			final List<Transaction> transactions,
			final BalanceValidationOptions options,
			final TransactionValidatorFactory validatorFactory,
			final PoiFacade poiFacade,
			final HashCache transactionHashCache) {
		this.validatorFactory = validatorFactory;
		this.poiFacade = poiFacade;
		this.transactionHashCache = transactionHashCache;
		this.singleValidator = this.createSingleValidator();
		for (final Transaction transaction : transactions) {
			this.add(transaction, options == BalanceValidationOptions.ValidateAgainstUnconfirmedBalance);
		}
	}

	private UnconfirmedTransactions filter(
			final List<Transaction> transactions,
			final BalanceValidationOptions options) {
		return new UnconfirmedTransactions(
				transactions,
				options,
				this.validatorFactory,
				this.poiFacade,
				this.transactionHashCache);
	}

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int size() {
		return this.transactions.size();
	}

	/**
	 * Gets the unconfirmed balance for the specified account.
	 *
	 * @param account The account.
	 * @return The unconfirmed balance.
	 */
	public Amount getUnconfirmedBalance(final Account account) {
		return this.unconfirmedBalances.get(account);
	}

	/**
	 * Adds new unconfirmed transactions.
	 *
	 * @param transactions The collection of transactions.
	 * @return SUCCESS if all transactions were added successfully, NEUTRAL or FAILURE otherwise.
	 */
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		final ValidationResult transactionValidationResult = this.validateBatch(transactions);
		if (!transactionValidationResult.isSuccess()) {
			return transactionValidationResult;
		}

		return ValidationResult.aggregate(transactions.stream().map(transaction -> this.add(transaction, true)).iterator());
	}

	/**
	 * Adds a new unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addNew(final Transaction transaction) {
		final ValidationResult transactionValidationResult = this.validateBatch(Arrays.asList(transaction));
		return transactionValidationResult.isSuccess()
				? this.add(transaction, true)
				: transactionValidationResult;
	}

	/**
	 * Adds an unconfirmed transaction that has been added previously (so some validation checks can be skipped).
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction, true);
	}

	private ValidationResult add(final Transaction transaction, final boolean execute) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (this.transactions.containsKey(transactionHash) || null != this.pendingTransactions.putIfAbsent(transactionHash, true)) {
			return ValidationResult.NEUTRAL;
		}

		try {
			return this.addInternal(transaction, transactionHash, execute);
		} finally {
			this.pendingTransactions.remove(transactionHash);
		}
	}

	private ValidationResult addInternal(
			final Transaction transaction,
			final Hash transactionHash,
			final boolean execute) {
		final ValidationResult validationResult = this.validateSingle(transaction);
		if (!validationResult.isSuccess()) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		this.transactions.put(transactionHash, transaction);
		return ValidationResult.SUCCESS;
	}

	private ValidationResult validateBatch(final Collection<Transaction> transactions) {
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, new ValidationContext());
		return this.validatorFactory.createBatch(this.transactionHashCache).validate(Arrays.asList(pair));
	}

	private ValidationResult validateSingle(final Transaction transaction) {
		return this.singleValidator.validate(
				transaction,
				new ValidationContext((account, amount) -> this.getUnconfirmedBalance(account).compareTo(amount) >= 0));
	}

	private SingleTransactionValidator createSingleValidator() {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		builder.add(this.validatorFactory.createSingle(this.poiFacade));
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		return builder.build();
	}

	/**
	 * Removes the specified transaction from the list of unconfirmed transactions.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was found and removed; false if the transaction was not found.
	 */
	public boolean remove(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (!this.transactions.containsKey(transactionHash)) {
			return false;
		}

		transaction.undo(this.transferObserver);

		this.transactions.remove(transactionHash);
		return true;
	}

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	public void removeAll(final Block block) {
		for (final Transaction transaction : block.getTransactions()) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);

			// don't call this.remove because transactions should not be removed when undone;
			// otherwise, the unconfirmed balances would not be correct
			this.transactions.remove(transactionHash);
		}
	}

	/**
	 * Gets all transactions.
	 *
	 * @return All transaction from this unconfirmed transactions.
	 */
	public List<Transaction> getAll() {
		final List<Transaction> transactions = this.transactions.values().stream()
				.collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	/**
	 * Gets all transactions up to a given limit of transactions.
	 *
	 * @return The list of unconfirmed transactions.
	 */
	public List<Transaction> getMostImportantTransactions(final int maxTransactions) {
		return this.transactions.values().stream()
				.sorted((lhs, rhs) -> -1 * lhs.compareTo(rhs))
				.limit(maxTransactions)
				.collect(Collectors.toList());
	}

	/**
	 * Gets all transactions before the specified time. Returned list is sorted.
	 *
	 * @param time The specified time.
	 * @return The sorted list of all transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions = this.transactions.values().stream()
				.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
				.collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
	}

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpiredTransactions(final TimeInstant time) {
		this.transactions.values().stream()
				.filter(tx -> tx.getDeadline().compareTo(time) < 0)
				.forEach(tx -> this.remove(tx));
	}

	/**
	 * Gets all transactions for the specified account.
	 *
	 * @param address The account address.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForAccount(final Address address) {
		return this.filter(
				this.getAll().stream()
						.filter(tx -> matchAddress(tx, address))
						.collect(Collectors.toList()),
				BalanceValidationOptions.ValidateAgainstUnconfirmedBalance);
	}

	private static boolean matchAddress(final Transaction transaction, final Address address) {
		if (transaction.getSigner().getAddress().equals(address)) {
			return true;
		}

		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER:
				return ((TransferTransaction)transaction).getRecipient().getAddress().equals(address);

			default:
				return false;
		}
	}

	/**
	 * Gets all the unconfirmed transactions that are eligible for inclusion into the next block.
	 *
	 * @param harvesterAddress The harvester's address.
	 * @param blockTime The block time.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForNewBlock(final Address harvesterAddress, final TimeInstant blockTime) {
		// in order for a transaction to be eligible for inclusion in a block, it must
		// (1) occur at or before the block time
		// (2) be signed by an account other than the harvester
		// (3) pass validation against the *confirmed* balance

		// this filter validates all transactions against confirmed balance:
		// a) we need to use unconfirmed balance to avoid some stupid situations (and spamming).
		// b) B has 0 balance, A->B 10nems, B->X 5nems with 2nem fee, since we check unconfirmed balance,
		//    both this TXes will get added, when creating a block, TXes are sorted by FEE,
		//    so B's TX will get on list before A's, and ofc it is invalid, and must get removed
		// c) we're leaving it in unconfirmedTxes, so it should be included in next block
		return this.filter(
				this.getTransactionsBefore(blockTime).stream()
						.filter(tx -> !tx.getSigner().getAddress().equals(harvesterAddress))
						.collect(Collectors.toList()),
				BalanceValidationOptions.ValidateAgainstConfirmedBalance);
	}

	private static class UnconfirmedBalancesObserver implements TransferObserver {
		private final Map<Account, Amount> unconfirmedBalances = new ConcurrentHashMap<>();

		public Amount get(final Account account) {
			return this.unconfirmedBalances.getOrDefault(account, account.getBalance());
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newBalance = this.unconfirmedBalances.get(account).add(amount);
			this.unconfirmedBalances.replace(account, newBalance);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newBalance = this.unconfirmedBalances.get(account).subtract(amount);
			this.unconfirmedBalances.replace(account, newBalance);
		}

		private void addToCache(final Account account) {
			// it's ok to put reference here, thanks to Account being non-mutable
			this.unconfirmedBalances.putIfAbsent(account, account.getBalance());
		}
	}
}
