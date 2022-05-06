package org.nem.nis.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.logging.Logger;

@ContextConfiguration(classes = TestConfHardDisk.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoITCase {
	private static final Logger LOGGER = Logger.getLogger(TransferDaoITCase.class.getName());

	@Autowired
	TransferDao transferDao;

	@Autowired
	TestDatabase database;

	@Test
	public void getTransactionsForAccountItCase() {
		// Arrange:
		final int numRounds = 10;
		this.database.load();

		// Act:
		this.getTransactionsForAccountSpeedTest(numRounds);
	}

	private void getTransactionsForAccountSpeedTest(final int numRounds) {
		final long start = System.currentTimeMillis();
		for (int i = 0; i < numRounds; i++) {
			this.transferDao.getTransactionsForAccountUsingId(
					this.database.getRandomAccount(),
					null,
					ReadOnlyTransferDao.TransferType.ALL,
					25);
		}

		final long stop = System.currentTimeMillis();
		LOGGER.warning(String.format("getTransactionsForAccountUsingId needed %dms", (stop - start) / numRounds));
	}
}
