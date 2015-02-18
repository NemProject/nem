package org.nem.nis.dao;

import org.hibernate.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

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
		final List<Account> accounts = this.database.getAccounts();

		// Act:
		this.getTransactionsForAccountSpeedTest(accounts, numRounds);
	}

	private void getTransactionsForAccountSpeedTest(final List<Account> accounts, final int numRounds) {
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
