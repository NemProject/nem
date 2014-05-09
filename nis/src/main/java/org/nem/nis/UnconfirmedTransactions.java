package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Account, Amount> unconfirmedBalances = new ConcurrentHashMap<>();
	private final TransferObserver transferObserver = new UnconfirmedTransactionsTransferObserver();

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
	boolean add(final Transaction transaction) {
		return this.add(transaction, hash -> false);
	}

	/**
	 * Adds an unconfirmed transaction if and only if the predicate evaluates to false.
	 *
	 * @param transaction The transaction.
	 * @param exists Predicate that determines the existence of the transaction given its hash.
	 * @return true if the transaction was added.
	 */
	boolean add(final Transaction transaction, final Predicate<Hash> exists) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (exists.test(transactionHash)) {
			return false;
		}

		if (this.transactions.containsKey(transactionHash)) {
			return false;
		}

		// isValid checks the sender balance
		if (!transaction.isValid()) {
			return false;
		}

		transaction.subscribe(this.transferObserver);
		transaction.execute(false);
		final Transaction previousTransaction = this.transactions.putIfAbsent(transactionHash, transaction);
		return null == previousTransaction;
	}

	/**
	 * Gets a value indicated whether or not this object is subscribed to the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the this object is subscribed to this transaction.
	 */
	boolean isSubscribed(final Transaction transaction) {
		return transaction.isSubscribed(this.transferObserver);
	}

	boolean remove(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (! this.transactions.containsKey(transactionHash)) {
			return false;
		}

		transaction.undo(false);
		transaction.unsubscribe(this.transferObserver);
		this.transactions.remove(transactionHash);
		return true;
	}

	private void addToCache(Account account) {
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
			final Transaction removedTransaction = this.transactions.remove(transactionHash);
			removedTransaction.unsubscribe(this.transferObserver);
		}
	}


	private List<Transaction> sortTransactions(List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> {
			// should we just use Transaction.compare (it weights things other than fees more heavily) ?
			// maybe we should change Transaction.compare? also it
			// TODO: should fee or time be more important inside Transaction.compare
			int result = -lhs.getFee().compareTo(rhs.getFee());
			if (result == 0) {
				result = lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
			}
			return result;
		});

		return transactions;
	}

	/**
	 * Gets all transactions before the specified time.
	 *
	 * @param time The specified time.
	 * @return All transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions =  this.transactions.values().stream()
				.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
				.collect(Collectors.toList());

		return sortTransactions(transactions);
	}

	/**
	 * Gets all transactions.
	 * @return All transaction from this unconfirmed transactions.
	 */
	public List<Transaction> getAll() {
		final List<Transaction> transactions =  this.transactions.values().stream()
				.collect(Collectors.toList());

		return sortTransactions(transactions);
	}


	/**
	 * There might be conflicting transactions on the list of unconfirmed transactions.
	 * This method iterates over *sorted* list of unconfirmed transactions, filtering out any conflicting ones.
	 * Currently conflicting transactions are NOT removed from main list of unconfirmed transactions.
	 *
	 * @param unconfirmedTransactions sorted list of unconfirmed transactions.
	 * @return filtered out list of unconfirmed transactions.
	 */
	public List<Transaction> removeConflictingTransactions(List<Transaction> unconfirmedTransactions) {
		final UnconfirmedTransactions filteredTxes = new UnconfirmedTransactions();

		// TODO: should we remove those that .add() failed?
		unconfirmedTransactions.stream()
				.forEach(filteredTxes::add);

		return filteredTxes.getAll();
	}

	/**
	 * drops transactions for which we are after the deadline already
	 *
	 * @param time
	 */
	public void dropExpiredTransactions(TimeInstant time) {
		this.transactions.values().stream()
				.filter(tx -> tx.getDeadline().compareTo(time) < 0)
				.forEach(this::remove);
	}

	private class UnconfirmedTransactionsTransferObserver implements TransferObserver {
		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			addToCache(account);
			final Amount newBalance = unconfirmedBalances.get(account).add(amount);
			unconfirmedBalances.replace(account, newBalance);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			addToCache(account);
			final Amount newBalance = unconfirmedBalances.get(account).subtract(amount);
			unconfirmedBalances.replace(account, newBalance);
		}
	}
}
