package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
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
	 * @param blockHeight The block height.
	 * @return The transactions.
	 */
	List<Transaction> getBlockTransactions(Address harvesterAddress, TimeInstant blockTime, BlockHeight blockHeight);
}
