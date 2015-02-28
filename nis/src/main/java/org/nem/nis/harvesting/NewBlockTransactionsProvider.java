package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.List;

/**
 * Provider of transactions for a new block.
 */
public interface NewBlockTransactionsProvider {

	/**
	 * Gets block transactions for the specified harvester and block parameters.
	 *
	 * @param harvesterAddress The harvester address.
	 * @param blockTime The block time.
	 * @return The transactions.
	 */
	List<Transaction> getBlockTransactions(
			Address harvesterAddress,
			TimeInstant blockTime);
}
