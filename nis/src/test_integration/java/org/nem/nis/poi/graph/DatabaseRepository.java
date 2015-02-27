package org.nem.nis.poi.graph;

import java.util.*;

/**
 * Interface for database repositories of blockchain data.
 */
public interface DatabaseRepository {

	static final Map<String, Collection<GraphClusteringTransaction>> transactionCache = new HashMap<>();

	/**
	 * Loads a subset of transactions.
	 *
	 * @param startHeight The starting block height (inclusive).
	 * @param stopHeight The ending block height (inclusive).
	 * @return The transactions.
	 */
	public Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight);
}
