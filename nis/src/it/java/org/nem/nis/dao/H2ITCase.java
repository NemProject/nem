package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import java.security.SecureRandom;
import java.sql.*;
import java.util.logging.Logger;
import java.util.stream.*;

public class H2ITCase {
	private static final Logger LOGGER = Logger.getLogger(H2ITCase.class.getName());

	@Ignore
	@Test
	@SuppressWarnings("deprecation")
	public void h2MemoryTest() {
		final H2Database db = new H2Database("test");
		final int range = 1000;
		for (int j = 0; j < 1000; j++) {
			if (j % 100 == 0) {
				LOGGER.info(String.format("round %d", j + 1));
			}
			final int randomInt = new SecureRandom().nextInt(range);
			final String ids = StringUtils.join(IntStream.range(1, 19000 + randomInt).mapToObj(Long::new).collect(Collectors.toList()), ",");
			final String sql = "SELECT a.* FROM accounts a WHERE a.id in (" + ids + ")";
			final ResultSet rs = db.executeQuery(sql);

			// do something with the result set so the compiler doesn't optimize it away.
			try {
				rs.beforeFirst();
				rs.last();
				final int size = rs.getRow();
				LOGGER.info(String.format("size %d", size));
			} catch (final SQLException e) {
				throw new RuntimeException("problem with result set in h2MemoryTest");
			}

			// can't make a meaningful assert here because the result of a full garbage collection is very variable
		}
	}
}
