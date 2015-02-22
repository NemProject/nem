package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.HashShortId;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Helper class that provides functions for filtering unconfirmed transactions.
 */
public class UnconfirmedTransactionsFilter {
	private final UnconfirmedTransactionsCache transactions;
	private final BiPredicate<Address, Transaction> matchesPredicate;

	/**
	 * Creates a new filter.
	 *
	 * @param transactions The transactions to filter.
	 */
	public UnconfirmedTransactionsFilter(
			final UnconfirmedTransactionsCache transactions,
			final BiPredicate<Address, Transaction> matchesPredicate) {
		this.transactions = transactions;
		this.matchesPredicate = matchesPredicate;
	}

	/**
	 * Gets all transactions.
	 *
	 * @return All transaction from this unconfirmed transactions.
	 */
	public List<Transaction> getAll() {
		final List<Transaction> transactions = this.transactions.stream()
				.collect(Collectors.toList());
		return this.sortTransactions(transactions);
	}

	/**
	 * Gets the transactions for which the hash short id is not in the given collection.
	 *
	 * @param knownHashShortIds The collection of known hashes.
	 * @return The unknown transactions.
	 */
	public List<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
		// probably faster to use hash map than collection
		final HashMap<HashShortId, Transaction> unknownHashShortIds = new HashMap<>(this.transactions.size());
		this.transactions.stream()
				.forEach(t -> unknownHashShortIds.put(new HashShortId(HashUtils.calculateHash(t).getShortId()), t));
		knownHashShortIds.stream().forEach(unknownHashShortIds::remove);
		return unknownHashShortIds.values().stream().collect(Collectors.toList());
	}

	/**
	 * Gets the most recent transactions of an account up to a given limit.
	 *
	 * @param address The address of an account.
	 * @param maxTransactions The maximum number of transactions.
	 * @return The most recent transactions from this unconfirmed transactions.
	 */
	public List<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions) {
		return this.transactions.stream()
				.filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE)
				.filter(tx -> this.matchesPredicate.test(address, tx))
				.sorted((t1, t2) -> -t1.getTimeStamp().compareTo(t2.getTimeStamp()))
				.limit(maxTransactions)
				.collect(Collectors.toList());
	}

	/**
	 * Gets all transactions up to a given limit of transactions.
	 *
	 * @param maxTransactions The maximum number of transactions.
	 * @return The list of unconfirmed transactions.
	 */
	public List<Transaction> getMostImportantTransactions(final int maxTransactions) {
		final int[] txCount = new int[1];
		return this.transactions.stream()
				.sorted((lhs, rhs) -> -1 * lhs.compareTo(rhs))
				.filter(t -> {
					txCount[0] += 1 + t.getChildTransactions().size();
					return maxTransactions >= txCount[0];
				})
				.collect(Collectors.toList());
	}

	/**
	 * Gets all transactions before the specified time. Returned list is sorted.
	 *
	 * @param time The specified time.
	 * @return The sorted list of all transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions = this.transactions.stream()
				.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
						// filter out signatures because we don't want them to be directly inside a block
				.filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE)
				.collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
	}
}
