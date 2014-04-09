package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.*;

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
	 * Adds an unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	boolean add(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);

		// TODO: add this back
//		synchronized (blockChain) {
//			Transfer tx = transferDao.findByHash(transactionHash.get());
//			if (tx != null) {
//				return false;
//			}
//		}

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
		final List<Transaction> transactions = new ArrayList<>();
		for (final Transaction tx : this.transactions.values()) {
			if (tx.getTimeStamp().compareTo(time) < 0) {
				transactions.add(tx);
			}
		}

		return transactions;
	}
}
