package org.nem.nis.poi.graph;

import java.util.Collection;

/**
 * Interface for database repositories of blockchain data.
 */
public interface DatabaseRepository {
	public Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight);
}
