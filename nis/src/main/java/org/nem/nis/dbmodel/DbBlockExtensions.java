package org.nem.nis.dbmodel;

import org.nem.nis.mappers.TransactionRegistry;

import java.util.List;

/**
 * Static helper class for dealing with db blocks.
 */
public class DbBlockExtensions {

	/**
	 * Counts the number of transactions in the block.
	 *
	 * @param block The block.
	 * @return The number of transactions.
	 */
	@SuppressWarnings("rawtypes")
	public static int countTransactions(final DbBlock block) {
		int numTransactions = 0;
		for (final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
			final List<AbstractBlockTransfer> transactions = entry.getFromBlock.apply(block);
			numTransactions += transactions.stream().mapToInt(entry.getTransactionCount::apply).sum();
		}

		return numTransactions;
	}
}
