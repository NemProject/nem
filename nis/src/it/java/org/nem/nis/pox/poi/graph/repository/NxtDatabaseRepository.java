package org.nem.nis.pox.poi.graph.repository;

import org.nem.core.utils.ExceptionUtils;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * A repository class for loading NXT objects from a database.
 */
public class NxtDatabaseRepository implements AutoCloseable, DatabaseRepository {
	private static final Logger LOGGER = Logger.getLogger(NxtDatabaseRepository.class.getName());
	private static final String JDBC_DRIVER = "org.h2.Driver";

	private final Connection conn;

	/**
	 * Creates a new repository.
	 */
	public NxtDatabaseRepository() {
		this(String.format("jdbc:h2:%s/nem/nxt_db/nxt", System.getProperty("user.home")));
	}

	/**
	 * Creates a new repository.
	 *
	 * @param path The database path.
	 */
	public NxtDatabaseRepository(final String path) {
		this.conn = ExceptionUtils.propagate(() -> {
			Class.forName(JDBC_DRIVER);
			return DriverManager.getConnection(path, "sa", "sa");
		});
	}

	@Override
	public void close() throws Exception {
		ExceptionUtils.propagateVoid(() -> {
			if (null != this.conn) {
				this.conn.close();
			}
		});
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

		final List<GraphClusteringTransaction> transactionData = new ArrayList<>();
		ExceptionUtils.propagateVoid(() -> {
			try (final Statement stmt = this.conn.createStatement()) {
				final String sql = String.format(
						"SELECT height, sender_id, recipient_id, amount FROM TRANSACTION WHERE height >= %d AND height <= %d ORDER BY height ASC",
						startHeight,
						stopHeight);
				final ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					final GraphClusteringTransaction transaction = new GraphClusteringTransaction(
							rs.getLong("HEIGHT"),
							rs.getLong("SENDER_ID"),
							rs.getLong("RECIPIENT_ID"),
							rs.getLong("AMOUNT"));
					transactionData.add(transaction);
				}
			}
		});

		LOGGER.info(String.format("%d transactions found!", transactionData.size()));
		return transactionData;
	}
}
