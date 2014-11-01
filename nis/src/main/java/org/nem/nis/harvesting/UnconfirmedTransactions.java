package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.*;
import org.nem.nis.secret.BlockChainConstants;
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
	private final TimeProvider timeProvider;
	private final TransactionValidator validator;

	private enum AddOptions {
		AllowNeutral,
		RejectNeutral
	}

	private enum ValidationOptions {
		ValidateAgainstConfirmedBalance,
		ValidateAgainstUnconfirmedBalance,
	}

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param timeProvider The time provider to use.
	 * @param validator The transaction validator to use.
	 */
	public UnconfirmedTransactions(
			final TimeProvider timeProvider,
			final SingleTransactionValidator validator) {
		this.timeProvider = timeProvider;

		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();
		builder.add(validator);
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		this.validator = builder.build();
	}

	private UnconfirmedTransactions(
			final List<Transaction> transactions,
			final ValidationOptions options,
			final TimeProvider timeProvider,
			final TransactionValidator validator) {
		this.timeProvider = timeProvider;
		this.validator = validator;
		for (final Transaction transaction : transactions) {
			this.add(transaction, AddOptions.AllowNeutral, options == ValidationOptions.ValidateAgainstUnconfirmedBalance);
		}
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
	 * Adds a new unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addNew(final Transaction transaction) {
		final TimeInstant currentTime = this.timeProvider.getCurrentTime();
		final TimeInstant entityTime = transaction.getTimeStamp();
		if (entityTime.compareTo(currentTime.addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0) {
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE;
		}

		return this.add(transaction, AddOptions.RejectNeutral, true);
	}

	/**
	 * Adds an unconfirmed transaction that has been added previously (so some validation checks can be skipped).
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction, AddOptions.AllowNeutral, true);
	}

	private ValidationResult add(final Transaction transaction, final AddOptions options, final boolean execute) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (this.transactions.containsKey(transactionHash) || null != this.pendingTransactions.putIfAbsent(transactionHash, true)) {
			return ValidationResult.NEUTRAL;
		}

		try {
			return this.addInternal(transaction, transactionHash, options, execute);
		} finally {
			this.pendingTransactions.remove(transactionHash);
		}
	}

	private ValidationResult addInternal(
			final Transaction transaction,
			final Hash transactionHash,
			final AddOptions options,
			final boolean execute) {
		final ValidationResult validationResult = this.validate(transaction);
		if (!isSuccess(validationResult, options)) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		this.transactions.put(transactionHash, transaction);
		return ValidationResult.SUCCESS;
	}

	private static boolean isSuccess(final ValidationResult result, final AddOptions options) {
		return (AddOptions.AllowNeutral == options && ValidationResult.NEUTRAL == result) || result.isSuccess();
	}

	private ValidationResult validate(final Transaction transaction) {
		return this.validator.validate(
				transaction,
				new ValidationContext((account, amount) -> this.getUnconfirmedBalance(account).compareTo(amount) >= 0));
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
		return new UnconfirmedTransactions(
				this.getAll().stream()
						.filter(tx -> matchAddress(tx, address))
						.collect(Collectors.toList()),
				ValidationOptions.ValidateAgainstUnconfirmedBalance,
				this.timeProvider,
				this.validator);
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
		// (1) occur at or after the block time
		// (2) be signed by an account other than the harvester
		// (3) pass validation against the *confirmed* balance

		// this filter validates all transactions against confirmed balance:
		// a) we need to use unconfirmed balance to avoid some stupid situations (and spamming).
		// b) B has 0 balance, A->B 10nems, B->X 5nems with 2nem fee, since we check unconfirmed balance,
		//    both this TXes will get added, when creating a block, TXes are sorted by FEE,
		//    so B's TX will get on list before A's, and ofc it is invalid, and must get removed
		// c) we're leaving it in unconfirmedTxes, so it should be included in next block
		return new UnconfirmedTransactions(
				this.getTransactionsBefore(blockTime).stream()
						.filter(tx -> !tx.getSigner().getAddress().equals(harvesterAddress))
						.collect(Collectors.toList()),
				ValidationOptions.ValidateAgainstConfirmedBalance,
				this.timeProvider,
				this.validator);
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
