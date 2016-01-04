package org.nem.nis.dao;

import org.junit.Test;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.*;
import org.nem.deploy.CommonStarter;

import java.io.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.List;
import java.util.logging.*;
import java.util.stream.*;

// NOTE: you need to create the database h2_speed_test in order to be able to run the test.
public class H2StorageSpeedITCase {
	private static final Logger LOGGER = Logger.getLogger(H2StorageSpeedITCase.class.getName());
	private static H2Database DB = new H2Database("h2_speed_test");
	private final static SecureRandom RANDOM = new SecureRandom();

	static {
		try {
			final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties");
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void h2StorageSpeedTest() throws SQLException {
		LOGGER.info("Emptying db...");
		emptyDb();
		LOGGER.info("Creating accounts, please have patience...");
		final List<Account> accounts = createAccounts(100_000);
		LOGGER.info("Saving accounts...");
		DB.enableAutoCommit(false);
		accountStorageSpeedTest(accounts);
		LOGGER.info("Saving block...");
		insertBlock();
		LOGGER.info("Creating transactions, please have patience...");
		final List<TransferTransaction> transactions = createTransactions(150_000, accounts);
		LOGGER.info("Saving transactions...");
		transactionStorageSpeedTest(transactions);
		LOGGER.info("Finished");
	}

	private static void accountStorageSpeedTest(final List<Account> accounts) throws SQLException {
		final int batchCount = 500;
		assert batchCount <= accounts.size() && 0 == accounts.size() % batchCount;
		final int batchSize = accounts.size() / batchCount;
		final long start = System.currentTimeMillis();
		for (int i = 0; i < batchCount; i++) {
			final String sql = createPreparedStatementForAccountInsert();
			final PreparedStatement statement = DB.getConnection().prepareStatement(sql);
			for (int j = 0; j < batchSize; j++) {
				statement.setString(1, accounts.get(i * batchSize + j).getAddress().toString());
				statement.setString(2, accounts.get(i * batchSize + j).getAddress().getPublicKey().toString());
				statement.addBatch();
			}

			statement.executeBatch();
			DB.commit();
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Saving %d accounts to db needed %d ms (%d μs/account)",
				accounts.size(),
				stop - start,
				(stop - start) * 1000 / accounts.size()));
	}

	private static void transactionStorageSpeedTest(final List<TransferTransaction> transactions) throws SQLException {
		final int batchCount = 5000;
		assert batchCount <= transactions.size() && 0 == transactions.size() % batchCount;
		final int batchSize = transactions.size() / batchCount;
		final long start = System.currentTimeMillis();
		for (int i = 0; i < batchCount; i++) {
			final String sql = createPreparedStatementForTransactionInsert();
			final PreparedStatement statement = DB.getConnection().prepareStatement(sql);
			for (int j = 0; j < batchSize; j++) {
				final TransferTransaction transaction = transactions.get(i * batchSize + j);
				statement.setLong(1, 1);
				statement.setLong(2, i * batchSize + j + 1);
				statement.setString(3, HashUtils.calculateHash(transaction.asNonVerifiable()).toString());
				statement.setInt(4, transaction.getVersion());
				statement.setLong(5, transaction.getFee().getNumMicroNem());
				statement.setInt(6, transaction.getTimeStamp().getRawTime());
				statement.setInt(7, transaction.getDeadline().getRawTime());
				statement.setLong(8, RANDOM.nextInt(1000) + 1);
				statement.setString(9, transaction.getSignature().toString());
				statement.setLong(10, RANDOM.nextInt(1000) + 1);
				statement.setInt(11, RANDOM.nextInt(1000));
				statement.setLong(12, transaction.getAmount().getNumMicroNem());
				statement.setLong(13, 0);
				statement.setInt(14, 1);
				statement.setString(15, "");
				statement.addBatch();
			}

			statement.executeBatch();
			DB.commit();
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Saving %d transactions to db needed %d ms (%d μs/transaction)",
				transactions.size(),
				stop - start,
				(stop - start) * 1000 / transactions.size()));
	}

	private static String createPreparedStatementForAccountInsert() {
		return "INSERT INTO accounts (printableKey, publicKey) VALUES (?, ?)";
	}

	private static String createPreparedStatementForTransactionInsert() {
		return "INSERT INTO transfers (blockId, id, transferHash, version, fee, timestamp, deadline, senderId, senderProof" +
				", recipientId, blkIndex, amount, referencedTransaction, messageType, messagePayload) VALUES " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	private static List<Account> createAccounts(final int count) {
		return IntStream.range(0, count).mapToObj(i -> new Account(new KeyPair())).collect(Collectors.toList());
	}

	private static List<TransferTransaction> createTransactions(final int count, final List<Account> accounts) {
		final TimeProvider timeProvider = new SystemTimeProvider();
		return IntStream.range(0, count)
				.mapToObj(i -> {
					final TransferTransaction transaction = new TransferTransaction(
							timeProvider.getCurrentTime(),
							accounts.get(RANDOM.nextInt(accounts.size())),
							accounts.get(RANDOM.nextInt(accounts.size())),
							Amount.fromNem(RANDOM.nextInt(100000)),
							null);
					transaction.setDeadline(timeProvider.getCurrentTime().addHours(1));
					transaction.setFee(Amount.fromNem(1000));
					transaction.sign();
					return transaction;
				})
				.collect(Collectors.toList());
	}

	private static void insertBlock() {
		final String sql =  "INSERT INTO blocks (id, version, prevBlockHash, blockHash, generationHash, timestamp, harvesterId" +
				", harvesterProof, HarvestedInName, height, totalFee, difficulty) VALUES (1, 2, '', '', '', 1, 1, '', 1, 1, 1, 1)";
		DB.execute(sql);
		DB.commit();
	}

	private static void emptyDb() {
		DB.execute("SET FOREIGN_KEY_CHECKS=0;");
		DB.execute("TRUNCATE TABLE accounts;");
		DB.execute("TRUNCATE TABLE transfers;");
		DB.execute("TRUNCATE TABLE transferredMosaics;");
		DB.execute("TRUNCATE TABLE blocks;");
		DB.execute("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1");
		DB.execute("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1");
		DB.execute("SET FOREIGN_KEY_CHECKS=1;");
	}
}
