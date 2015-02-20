package org.nem.nis.poi.graph;

import java.util.*;
import java.util.logging.Logger;

/**
 * A repository class for loading the transactions from the Bitcoin blockchain.
 */
public class NemDatabaseRepository implements DatabaseRepository {
	private static final Logger LOGGER = Logger.getLogger(BtcDatabaseRepository.class.getName());

	private final Map<String, Collection<GraphClusteringTransaction>> transactionCache = new HashMap<>();

	/**
	 * Creates a new repository.
	 */
	public NemDatabaseRepository() {
		this(System.getProperty("user.home") + "/nem/nem_db");
	}

	/**
	 * Creates a new repository.
	 *
	 * @param path The database path.
	 */
	public NemDatabaseRepository(final String path) {

	}

	/**
	 * Loads all transactions from blocks with heights between startHeight and stopHeight, inclusive.
	 *
	 * @param startHeight The start height.
	 * @param stopHeight The stop height.
	 * @return The transactions.
	 */
	public Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		LOGGER.info(String.format("loading transactions in blocks [%d, %d]...", startHeight, stopHeight));

		final String transCacheKey = startHeight + "_" + stopHeight;
		if (this.transactionCache.containsKey(transCacheKey)) {
			return this.transactionCache.get(transCacheKey);
		}
		final List<GraphClusteringTransaction> transactionData = new ArrayList<>();
		// TODO: implement

		this.transactionCache.put(transCacheKey, transactionData);

		return transactionData;
	}
}
