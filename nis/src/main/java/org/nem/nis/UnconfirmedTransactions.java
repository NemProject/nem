package org.nem.nis;

import org.nem.core.model.*;
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

		final Transaction previousTransaction = this.transactions.putIfAbsent(transactionHash, transaction);
		return null == previousTransaction;
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
}
