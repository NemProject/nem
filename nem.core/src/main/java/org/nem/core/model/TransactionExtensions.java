package org.nem.core.model;

import java.util.*;
import java.util.stream.Stream;

/**
 * Static helper class for dealing with transactions.
 */
public class TransactionExtensions {

	/**
	 * Gets the child signatures for the specified transactions.
	 *
	 * @param transaction The transaction.
	 * @return The child signatures.
	 */
	public static Stream<Transaction> getChildSignatures(final Transaction transaction) {
		return transaction.getChildTransactions().stream()
				.filter(t -> TransactionTypes.MULTISIG_SIGNATURE == t.getType());
	}

	/**
	 * Streams transactions the default way (this is equivalent to streamSelfAndFirstChildTransactions).
	 *
	 * @param transaction The transaction.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamDefault(final Transaction transaction) {
		return streamSelfAndFirstChildTransactions(transaction);
	}

	/**
	 * Streams the transaction and all first level child transactions.
	 *
	 * @param transaction The transaction.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamSelfAndFirstChildTransactions(final Transaction transaction) {
		return Stream.concat(Stream.of(transaction), transaction.getChildTransactions().stream());
	}

	/**
	 * Streams the transaction, first level child transactions, and nth level child transactions.
	 * <br>
	 * This function is not needed currently.
	 *
	 * @param transaction The transaction.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamSelfAndAllTransactions(final Transaction transaction) {
		final List<Transaction> allTransactions = new ArrayList<>();
		allTransactions.add(transaction);
		addTransactionsRecursive(allTransactions, transaction.getChildTransactions());
		return allTransactions.stream();
	}

	private static void addTransactionsRecursive(final List<Transaction> transactions, final Collection<Transaction> source) {
		transactions.addAll(source);

		for (final Transaction t : source) {
			addTransactionsRecursive(transactions, t.getChildTransactions());
		}
	}
}
