package org.nem.core.model;

import java.util.*;
import java.util.stream.Stream;

/**
 * Static helper class for dealing with blocks.
 */
public class BlockExtensions {

	/**
	 * Streams transactions the default way (this is equivalent to streamDirectAndFirstChildTransactions).
	 *
	 * @param block The block.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamDefault(final Block block) {
		return streamDirectAndFirstChildTransactions(block);
	}

	/**
	 * Streams all direct block transactions.
	 *
	 * @param block The block.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamDirectTransactions(final Block block) {
		return block.getTransactions().stream();
	}

	/**
	 * Streams all direct block transactions and first level child transactions.
	 *
	 * @param block The block.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamDirectAndFirstChildTransactions(final Block block) {
		return Stream.concat(
				block.getTransactions().stream(),
				block.getTransactions().stream().flatMap(t -> t.getChildTransactions().stream()));
	}

	/**
	 * Streams all direct block transactions, first level child transactions, and nth level child transactions.
	 * <br/>
	 * This function is not needed currently.
	 *
	 * @param block The block.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamAllTransactions(final Block block) {
		final List<Transaction> allTransactions = new ArrayList<>();
		addTransactionsRecursive(allTransactions, block.getTransactions());
		return allTransactions.stream();
	}

	private static void addTransactionsRecursive(final List<Transaction> transactions, final Collection<Transaction> source) {
		transactions.addAll(source);

		for (final Transaction t : source) {
			addTransactionsRecursive(transactions, t.getChildTransactions());
		}
	}
}
