package org.nem.core.model;

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
		return block.getTransactions().stream().flatMap(TransactionExtensions::streamSelfAndFirstChildTransactions);
	}

	/**
	 * Streams all direct block transactions, first level child transactions, and nth level child transactions.
	 * <br>
	 * This function is not needed currently.
	 *
	 * @param block The block.
	 * @return The transactions.
	 */
	public static Stream<Transaction> streamAllTransactions(final Block block) {
		return block.getTransactions().stream().flatMap(TransactionExtensions::streamSelfAndAllTransactions);
	}
}
