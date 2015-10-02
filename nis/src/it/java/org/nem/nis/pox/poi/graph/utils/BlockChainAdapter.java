package org.nem.nis.pox.poi.graph.utils;

import org.nem.core.model.Address;
import org.nem.nis.pox.poi.graph.repository.GraphClusteringTransaction;
import org.nem.nis.state.AccountState;

import java.util.*;
import java.util.function.Function;

/**
 * An block chain adapter.
 */
public interface BlockChainAdapter {

	/**
	 * Gets the default end height.
	 *
	 * @return The default end height.
	 */
	int getDefaultEndHeight();

	/**
	 * Gets the block chain type.
	 *
	 * @return The block chain type.
	 */
	String getBlockChainType();

	/**
	 * Creates account states from transaction data.
	 *
	 * @param transactions The transaction data.
	 * @param normalizeAmount A function for normalizing an amount.
	 * @return The account states.
	 */
	Map<Address, AccountState> createAccountStatesFromTransactionData(
			final Collection<GraphClusteringTransaction> transactions,
			final Function<Long, Long> normalizeAmount);

	/**
	 * Gets the number of units of the implementation-specific currency.
	 *
	 * @return The number of units.
	 */
	long getSupplyUnits();

	/**
	 * Gets the market cap of the implementation-specific currency.
	 *
	 * @return The market cap.
	 */
	long getMarketCap();
}
