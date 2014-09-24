package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.validators.*;

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
	private final TransactionValidator validator;

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validator The transaction validator to use.
	 */
	public UnconfirmedTransactions(final TransactionValidator validator) {
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();
		builder.add(validator);
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		this.validator = builder.build();
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
	 * Adds an unconfirmed transaction if it has a SUCCESS validation result.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addValid(final Transaction transaction) {
		return this.add(transaction, false, true);
	}

	/**
	 * Adds an unconfirmed transaction if it has a SUCCESS or NEUTRAL validation result.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addValidOrNeutral(final Transaction transaction) {
		return this.add(transaction, true, true);
	}

	/**
	 * Adds an unconfirmed transaction if and only if the predicate evaluates to false.
	 *
	 * @param transaction The transaction.
	 * @param allowNeutral true if transactions should be considered valid if their validation result is NEUTRAL.
	 * @param execute true if valid transactions should be executed.
	 * @return true if the transaction was added.
	 */
	private ValidationResult add(final Transaction transaction, final boolean allowNeutral, final boolean execute) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (this.transactions.containsKey(transactionHash)) {
			return ValidationResult.NEUTRAL;
		}

		// not sure if adding to cache here is a good idea...
		this.addToCache(transaction.getSigner());
		final ValidationResult validationResult = this.validate(transaction);
		if (!validationResult.isSuccess() && (!allowNeutral || ValidationResult.NEUTRAL != validationResult)) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		// TODO 20140922 J-G above the transaction result is NEUTRAL but here it is FAILURE_HASH_EXISTS
		// TODO should (1) HASH_EXISTS be neutral status or should we use NEUTRAL here too?
		// TODO 20140923 G-J I think there might be something wrong with this method. Basically, the only
		// way, that check below would return FAILURE_HASH_EXISTS, is that if there was parallel add(),
		// which succeeded first, but we probably wouldn't like .execute() above to be called twice for the same TX.
		// OTOH making this method synchronized is rather poor idea, as most of the code could run in parallel
		// (i.e. for different hashes) Should we try to solve it, and if so any idea how?
		// maybe we should add hash to this.transactions on the top of the func, and here only replace it?
		// review!
		final Transaction previousTransaction = this.transactions.putIfAbsent(transactionHash, transaction);
		return null == previousTransaction ? ValidationResult.SUCCESS : ValidationResult.FAILURE_HASH_EXISTS;
	}

	private ValidationResult validate(final Transaction transaction) {
		return this.validator.validate(
				transaction,
				new ValidationContext((account, amount) -> this.unconfirmedBalances.get(account).compareTo(amount) >= 0));
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
		// UnconfirmedTransactions CAN contain conflicting TXes:
		// a) we need to use unconfirmed balance to avoid some stupid situations (and spamming).
		// b) B has 0 balance, A->B 10nems, B->X 5nems with 2nem fee, since we check unconfirmed balance,
		//    both this TXes will get added, when creating a block, TXes are sorted by FEE,
		//    so B's TX will get on list before A's, and ofc it is invalid, and must get removed
		// c) we're leaving it in unconfirmedTxes, so it should be included in next block
		// TODO-CR 20140922 J-G: it would be great to add a test for this exact case ;)
		final UnconfirmedTransactions filteredTxes = new UnconfirmedTransactions(this.validator);

		unconfirmedTransactions.stream()
				.forEach(tx -> filteredTxes.add(tx, true, false));

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
