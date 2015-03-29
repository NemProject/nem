package org.nem.nis.poi.graph.repository;

import java.util.*;

/**
 * A cached database repository decorator.
 */
public class CachedDatabaseRepository implements DatabaseRepository {
	private final DatabaseRepository repository;
	private final Map<String, Collection<GraphClusteringTransaction>> transactionCache = new HashMap<>();

	/**
	 * Creates a new repository.
	 *
	 * @param repository The inner repository.
	 */
	public CachedDatabaseRepository(final DatabaseRepository repository) {
		this.repository = repository;
	}

	@Override
	public Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		final String transCacheKey = startHeight + "_" + stopHeight;
		if (this.transactionCache.containsKey(transCacheKey)) {
			return this.transactionCache.get(transCacheKey);
		}

		final Collection<GraphClusteringTransaction> transactionData = this.repository.loadTransactionData(startHeight, stopHeight);
		this.transactionCache.put(transCacheKey, transactionData);
		return transactionData;
	}
}
