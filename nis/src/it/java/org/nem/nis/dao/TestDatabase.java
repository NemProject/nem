package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.deploy.CommonStarter;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

@ContextConfiguration(classes = TestConfHardDisk.class)
public class TestDatabase {
	private static final Logger LOGGER = Logger.getLogger(TestDatabase.class.getName());

	// you can force repopulating the database by replacing false with true in the next line
	private static final boolean SHOULD_POPULATE_DATABASE = !databaseFileExists();

	public static final int NUM_BLOCKS = 5000;
	private static final int NUM_TRANSACTIONS_PER_BLOCK = 100;
	private static final int NUM_ACCOUNTS = 100;

	static {
		try {
			final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties");
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Autowired
	@SuppressWarnings("CanBeFinal")
	private BlockDao blockDao;

	@Autowired
	@SuppressWarnings("CanBeFinal")
	private SessionFactory sessionFactory;

	private List<Account> accounts;

	/**
	 * Initializes and loads the database.
	 */
	public void load() {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		if (SHOULD_POPULATE_DATABASE) {
			this.accounts = this.createAccounts(NUM_ACCOUNTS, mockAccountDao);
			this.populateDatabase(NUM_BLOCKS, NUM_TRANSACTIONS_PER_BLOCK, accountDaoLookup);
		} else {
			this.accounts = this.readAccounts(mockAccountDao);
		}
	}

	/**
	 * Gets a random account.
	 *
	 * @return The account.
	 */
	public Account getRandomAccount() {
		final SecureRandom random = new SecureRandom();
		return this.accounts.get(random.nextInt(this.accounts.size()));
	}

	private static boolean databaseFileExists() {
		final File file = new File(System.getProperty("user.home") + "\\nem\\nis\\data\\test.h2.db");
		return file.exists();
	}

	private void populateDatabase(
			final int numBlocks,
			final int numTransactionsPerBlock,
			final AccountDaoLookup accountDaoLookup) {
		this.resetDatabase();
		final List<TransactionAccountSet> accountSets = this.createAccountSets(100, this.accounts);
		for (int i = 0; i < numBlocks; i++) {
			final DbBlock dbBlock = this.createBlock(
					i,
					numTransactionsPerBlock,
					this.getRandomAccount(),
					accountSets,
					accountDaoLookup);
			this.blockDao.save(dbBlock);
			if ((i + 1) % 100 == 0) {
				LOGGER.warning(String.format("Block %d", i + 1));
			}
		}
	}

	private void resetDatabase() {
		final Session session = this.sessionFactory.openSession();
		DbTestUtils.dbCleanup(session);
		session.close();
	}

	private DbBlock createBlock(
			final int round,
			final int numTransactions,
			final Account harvester,
			final List<TransactionAccountSet> accountSets,
			final AccountDaoLookup accountDaoLookup) {
		final SecureRandom random = new SecureRandom();
		final Block block = new Block(
				harvester,
				Hash.ZERO,
				Hash.ZERO,
				new TimeInstant(round * 123),
				new BlockHeight(round + 1));
		for (int j = 0; j < numTransactions; j++) {
			// distribution of transactions:
			// 80% transfer transactions
			// 15% multisig transfer transactions
			//  5% importance transfer transactions
			final TransactionAccountSet accountSet = this.getRandomAccountSet(accountSets);
			final int type = random.nextInt(100);
			if (80 > type) {
				final TransferTransaction transferTransaction = this.createTransferTransaction(
						accountSet.transferSender,
						accountSet.transferRecipient,
						numTransactions * round + j);
				block.addTransaction(transferTransaction);
			} else if (95 > type) {
				block.addTransaction(this.createMultisigTransferTransaction(
						accountSet.transferSender,
						accountSet.transferRecipient,
						accountSet.multisigSender,
						accountSet.cosignatory1,
						accountSet.cosignatory2,
						numTransactions * round + j));
			} else {
				block.addTransaction(this.createImportanceTransferTransaction(
						accountSet.transferSender,
						accountSet.transferRecipient,
						ImportanceTransferMode.Activate,
						numTransactions * round + j));
			}
		}
		block.sign();
		return MapperUtils.toDbModel(block, accountDaoLookup);
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final DbAccount dbSender = new DbAccount(account.getAddress());
		mockAccountDao.addMapping(account, dbSender);
	}

	private List<Account> readAccounts(final MockAccountDao mockAccountDao) {
		LOGGER.warning("reading accounts");
		final Session session = this.sessionFactory.openSession();
		final Query query = session.createQuery("from DbAccount a");
		final List<DbAccount> dbAccounts = HibernateUtils.listAndCast(query);
		session.flush();
		session.clear();
		session.close();
		final List<Account> accounts = dbAccounts.stream()
				.map(dbAccount -> {
					final Account account = new Account(new KeyPair(dbAccount.getPublicKey()));
					this.addMapping(mockAccountDao, account);
					return account;
				})
				.collect(Collectors.toList());
		LOGGER.warning("reading accounts finishes");
		return accounts;
	}

	private List<Account> createAccounts(final int numAccounts, final MockAccountDao mockAccountDao) {
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < numAccounts; i++) {
			final Account account = Utils.generateRandomAccount();
			accounts.add(account);
			this.addMapping(mockAccountDao, account);
		}

		return accounts;
	}

	private List<TransactionAccountSet> createAccountSets(final int numAccountSets, final List<Account> accounts) {
		final SecureRandom random = new SecureRandom();
		final List<TransactionAccountSet> accountSets = new ArrayList<>();
		final HashSet<Account> chosenAccounts = new HashSet<>();
		for (int i = 0; i < numAccountSets; i++) {
			chosenAccounts.clear();
			while (chosenAccounts.size() < 5) {
				chosenAccounts.add(accounts.get(random.nextInt(accounts.size())));
			}

			accountSets.add(new TransactionAccountSet(chosenAccounts.stream().collect(Collectors.toList())));
		}

		return accountSets;
	}

	private TransactionAccountSet getRandomAccountSet(final List<TransactionAccountSet> accountSets) {
		final SecureRandom random = new SecureRandom();
		return accountSets.get(random.nextInt(accountSets.size()));
	}

	private TransferTransaction createTransferTransaction(
			final Account sender,
			final Account recipient,
			final int i) {
		// Arrange:
		final TransferTransaction transaction = new TransferTransaction(
				new TimeInstant(i),
				sender,
				recipient,
				Amount.fromNem(123),
				null);
		transaction.sign();
		return transaction;
	}

	private ImportanceTransferTransaction createImportanceTransferTransaction(
			final Account sender,
			final Account recipient,
			final ImportanceTransferMode mode,
			final int i) {
		// Arrange:
		final ImportanceTransferTransaction transaction = new ImportanceTransferTransaction(
				new TimeInstant(i),
				sender,
				mode,
				recipient);
		transaction.sign();
		return transaction;
	}

	private MultisigTransaction createMultisigTransferTransaction(
			final Account transferSender,
			final Account transferRecipient,
			final Account multisigSender,
			final Account cosignatory1,
			final Account cosignatory2,
			final int i) {
		final TransferTransaction otherTransaction = new TransferTransaction(
				new TimeInstant(i),
				transferSender,
				transferRecipient,
				Amount.fromNem(123),
				null);
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, multisigSender, otherTransaction);
		this.addSignature(cosignatory1, transaction);
		this.addSignature(cosignatory2, transaction);
		transaction.sign();
		return transaction;
	}

	private void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				signatureSigner,
				multisigTransaction.getOtherTransaction().getSigner(),
				multisigTransaction.getOtherTransaction());
		signatureTransaction.sign();
		multisigTransaction.addSignature(signatureTransaction);
	}

	private class TransactionAccountSet {
		private final Account transferSender;
		private final Account transferRecipient;
		private final Account multisigSender;
		private final Account cosignatory1;
		private final Account cosignatory2;

		private TransactionAccountSet(final List<Account> accounts) {
			this.transferSender = accounts.get(0);
			this.transferRecipient = accounts.get(1);
			this.multisigSender = accounts.get(2);
			this.cosignatory1 = accounts.get(3);
			this.cosignatory2 = accounts.get(4);
		}
	}
}
