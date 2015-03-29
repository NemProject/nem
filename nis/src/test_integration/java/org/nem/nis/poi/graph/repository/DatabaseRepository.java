package org.nem.nis.poi.graph.repository;

import java.util.Collection;

/**
 * Interface for database repositories of blockchain data.
 */
public interface DatabaseRepository {

	/**
	 * Loads a subset of transactions.
	 *
	 * @param startHeight The starting block height (inclusive).
	 * @param stopHeight The ending block height (inclusive).
	 * @return The transactions.
	 */
	Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight);
}
