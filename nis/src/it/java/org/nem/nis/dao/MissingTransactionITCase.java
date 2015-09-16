package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.deploy.CommonStarter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class MissingTransactionITCase {
	private static final Logger LOGGER = Logger.getLogger(TestDatabase.class.getName());

	static {
		try {
			final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties");
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void noTransactionIsMissing() throws Exception {
		// supply appropriate data here:
		// the NetworkSpammer creates transactions with increasing amounts.
		// default start value for an amount is 1
		this.assertNoTransactionIsMissing(400, 430, 1, 10_000);
	}

	private void assertNoTransactionIsMissing(
			final long startHeight,
			final long endHeight,
			final long startAmount,
			final long endAmount) throws Exception {
		final H2Database db = new H2Database("nis5_mijinnet");
		final long startId = this.getBlockId(db, startHeight);
		final long endId = this.getBlockId(db, endHeight);
		final Set<Long> amounts = LongStream.range(startAmount, endAmount + 1).boxed().collect(Collectors.toSet());
		final String sql = String.format("SELECT amount FROM transfers WHERE blockId >= %d AND blockId <= %d",
				startId,
				endId);
		final ResultSet rs = db.executeQuery(sql);
		while (rs.next()) {
			amounts.remove(rs.getLong("amount"));
		}

		rs.close();
		LOGGER.info(String.format("%d transactions are missing", amounts.size()));
		if (0 < amounts.size()) {
			LOGGER.info("The first 10 missing transactions have amounts:");
			LOGGER.info(amounts.stream()
					.sorted(Long::compareTo)
					.limit(10)
					.map(a -> Long.toString((a)))
					.collect(Collectors.joining(", ")));
		}

		db.close();
		Assert.assertThat(amounts.size(), IsEqual.equalTo(0));
	}

	private long getBlockId(final H2Database db, final long height) throws SQLException {
		final String sql = String.format("SELECT id FROM blocks WHERE height=%d", height);
		final ResultSet rs = db.executeQuery(sql);
		if (rs.next()) {
			final long id = rs.getLong("id");
			rs.close();
			return id;
		} else {
			throw new RuntimeException(String.format("block height %d not found in database", height));
		}
	}
}
