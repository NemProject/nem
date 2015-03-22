package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import java.security.SecureRandom;
import java.sql.*;
import java.util.logging.Logger;
import java.util.stream.*;

public class H2Test {
	private static final Logger LOGGER = Logger.getLogger(H2Test.class.getName());

	@Ignore
	@Test
	public void h2MemoryTest() {
		final H2Database db = new H2Database();
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
			} catch (final SQLException e) {
				throw new RuntimeException("problem with result set in h2MemoryTest");
			}

			// TODO 20150320 J-B: should we have an assert here? e.g. the delta in used memory is less than some value?
			// TODO 20150320 J-B: also can we cut down on the iterations on the dao tests, in general?
			// TODO 20150322 BR -> J: not sure if we can make a meaningful assert here. If you look at how successful a full garbage collection is
			// > you see that it varies a lot.
		}
	}
}
