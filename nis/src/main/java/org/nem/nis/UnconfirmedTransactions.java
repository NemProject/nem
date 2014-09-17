package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactions.class.getName());

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Account, Amount> unconfirmedBalances = new ConcurrentHashMap<>();
	private final TransactionObserver transferObserver = new TransferObserverToTransactionObserverAdapter(new UnconfirmedTransactionsTransferObserver());

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int size() {
		return this.transactions.size();
	}

	/**
	 * Adds an unconfirmed transaction unconditionally.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult add(final Transaction transaction) {
		return this.add(transaction, hash -> false);
	}

	/**
	 * Adds an unconfirmed transaction if and only if the predicate evaluates to false.
	 *
	 * @param transaction The transaction.
	 * @param exists Predicate that determines the existence of the transaction given its hash.
	 * @return true if the transaction was added.
	 */
	public ValidationResult add(final Transaction transaction, final Predicate<Hash> exists) {
		return this.add(transaction, exists, true);
	}

	/**
	 * Adds an unconfirmed transaction if and only if the predicate evaluates to false.
	 *
	 * @param transaction The transaction.
	 * @param exists Predicate that determines the existence of the transaction given its hash.
	 * @param execute determines if the transaction should be executed if valid.
	 * @return true if the transaction was added.
	 */
	private ValidationResult add(final Transaction transaction, final Predicate<Hash> exists, final boolean execute) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (exists.test(transactionHash)) {
			return ValidationResult.NEUTRAL;
		}

		if (this.transactions.containsKey(transactionHash)) {
			return ValidationResult.NEUTRAL;
		}

		// not sure if adding to cache here is a good idea...
		this.addToCache(transaction.getSigner());
		if (!this.isValid(transaction)) {
			LOGGER.warning(String.format("Transaction from %s rejected (invalid).", transaction.getSigner().getAddress()));
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		final Transaction previousTransaction = this.transactions.putIfAbsent(transactionHash, transaction);
		return null == previousTransaction ? ValidationResult.SUCCESS : ValidationResult.FAILURE_HASH_EXISTS;
	}

	private boolean isValid(final Transaction transaction) {
		return ValidationResult.SUCCESS == transaction.checkValidity(
				(account, amount) -> this.unconfirmedBalances.get(account).compareTo(amount) >= 0);
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

	private void addToCache(final Account account) {
		// it's ok to put reference here, thanks to Account being non-mutable
		this.unconfirmedBalances.putIfAbsent(account, account.getBalance());
	}

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	void removeAll(final Block block) {
		for (final Transaction transaction : block.getTransactions()) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			this.transactions.remove(transactionHash);
		}
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
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
	 * Executes all transactions.
	 */
	private void executeAll() {
		this.getAll().stream()
				.forEach(tx -> tx.execute(this.transferObserver));
	}

	/**
	 * There might be conflicting transactions on the list of unconfirmed transactions.
	 * This method iterates over *sorted* list of unconfirmed transactions, filtering out any conflicting ones.
	 * Currently conflicting transactions are NOT removed from main list of unconfirmed transactions.
	 *
	 * @param unconfirmedTransactions sorted list of unconfirmed transactions.
	 * @return filtered out list of unconfirmed transactions.
	 */
	public List<Transaction> removeConflictingTransactions(final List<Transaction> unconfirmedTransactions) {
		final UnconfirmedTransactions filteredTxes = new UnconfirmedTransactions();

		// TODO: should we remove those that .add() failed?
		unconfirmedTransactions.stream()
				.forEach(tx -> filteredTxes.add(tx, hash -> false, false));

		filteredTxes.executeAll();
		return filteredTxes.getAll();
	}

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpiredTransactions(final TimeInstant time) {
		this.transactions.values().stream()
				.filter(tx -> tx.getDeadline().compareTo(time) < 0)
				.forEach(obj -> this.remove(obj));
	}

	private class UnconfirmedTransactionsTransferObserver implements TransferObserver {
		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			UnconfirmedTransactions.this.addToCache(account);
			final Amount newBalance = UnconfirmedTransactions.this.unconfirmedBalances.get(account).add(amount);
			UnconfirmedTransactions.this.unconfirmedBalances.replace(account, newBalance);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			UnconfirmedTransactions.this.addToCache(account);
			final Amount newBalance = UnconfirmedTransactions.this.unconfirmedBalances.get(account).subtract(amount);
			UnconfirmedTransactions.this.unconfirmedBalances.replace(account, newBalance);
		}
	}
}
