package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.*;

@Ignore
@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class TransactionRetrieverTest {
	protected static final long TRANSACTIONS_PER_BLOCK = 16L;
	private static final int LIMIT = 10;
	private AtomicInteger counter = new AtomicInteger(0);
	private static final Account[] ACCOUNTS = {
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount()
	};

	@Autowired
	AccountDao accountDao;

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void createDb() {
		this.session = this.sessionFactory.openSession();
		setupBlocks();
	}

	@After
	public void destroyDb() {
		session.createSQLQuery("delete from multisigsignatures").executeUpdate();
		session.createSQLQuery("delete from multisigtransactions").executeUpdate();
		session.createSQLQuery("delete from transfers").executeUpdate();
		session.createSQLQuery("delete from importancetransfers").executeUpdate();
		session.createSQLQuery("delete from multisigmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsignermodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsends").executeUpdate();
		session.createSQLQuery("delete from multisigreceives").executeUpdate();
		session.createSQLQuery("delete from blocks").executeUpdate();
		session.createSQLQuery("delete from accounts").executeUpdate();
		session.createSQLQuery("ALTER SEQUENCE transaction_id_seq RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigmodifications ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigsends ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigreceives ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1").executeUpdate();
		this.session.flush();
		this.session.clear();

		this.session.close();
	}

	/**
	 * Gets the transaction retriever.
	 * Must be implemented by subclass.
	 *
	 * @return The transaction retriever.
	 */
	protected abstract TransactionRetriever getTransactionRetriever();

	/**
	 * Gets the list of expected ids for incoming transfers.
	 *
	 * @param height The block height.
	 * @param accountIndex The account index in the array.
	 * @return The expected ids.
	 */
	protected abstract List<Integer> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex);

	/**
	 * Gets the list of expected ids for outgoing transfers.
	 *
	 * @param height The block height.
	 * @param accountIndex The account index in the array.
	 * @return The expected ids.
	 */
	protected abstract List<Integer> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex);

	@Test
	public void canRetrieveIncomingTransactionsFromBeginning() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, 34));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromBeginning() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, 34));
	}

	@Test
	public void canRetrieveIncomingTransactionsFromMiddle() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, 234));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromMiddle() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, 234));
	}

	@Test
	public void canRetrieveIncomingTransactionsFromEnd() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, Long.MAX_VALUE));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromEnd() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, Long.MAX_VALUE));
	}

	private void assertCanRetrieveIncomingTransactions(final int accountIndex, final long topMostId) {
		// Arrange:
		final TransactionRetriever retriever = getTransactionRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(
				this.session,
				this.getAccountId(ACCOUNTS[accountIndex]),
				topMostId,
				LIMIT,
				ReadOnlyTransferDao.TransferType.INCOMING);
		final Collection<Integer> ids = pairs.stream()
				.map(p -> (int)p.getTransfer().getId().longValue())
				.collect(Collectors.toList());

		// Assert:
		final Collection<Integer> expectedIds = getExpectedIdsForTransactions(
				this::getExpectedComparablePairsForIncomingTransactions,
				accountIndex,
				topMostId,
				LIMIT) ;
		Assert.assertThat(ids, IsEquivalent.equivalentTo(expectedIds));
	}

	private void assertCanRetrieveOutgoingTransactions(final int accountIndex, final long topMostId) {
		// Arrange:
		final TransactionRetriever retriever = getTransactionRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(
				this.session,
				this.getAccountId(ACCOUNTS[accountIndex]),
				topMostId,
				LIMIT,
				ReadOnlyTransferDao.TransferType.OUTGOING);
		final Collection<Integer> ids = pairs.stream()
				.map(p -> (int)p.getTransfer().getId().longValue())
				.collect(Collectors.toList());

		// Assert:
		final Collection<Integer> expectedIds = getExpectedIdsForTransactions(
				this::getExpectedComparablePairsForOutgoingTransactions,
				accountIndex,
				topMostId,
				LIMIT);
		Assert.assertThat(ids, IsEquivalent.equivalentTo(expectedIds));
	}

	private List<Integer> getExpectedIdsForTransactions(
			final BiFunction<BlockHeight, Integer, List<Integer>> expectedIdsSupplier,
			final int accountIndex,
			final long topMostTransactionId,
			final int limit) {
		List<Integer> ids = new ArrayList<>();
		final long adjustedId = Math.max(topMostTransactionId - 2, 0);
		long height = Math.min(Math.max((adjustedId / TRANSACTIONS_PER_BLOCK + 1) * 2, 1), 50);
		while (limit > ids.size() && height > 0) {
			ids.addAll(expectedIdsSupplier.apply(new BlockHeight(height), accountIndex));
			height -= 2L;
			ids = ids.stream().filter(id -> id < topMostTransactionId).collect(Collectors.toList());
		}

		return ids.subList(0, limit > ids.size() ? ids.size() : limit);
	}

	protected void setupBlocks() {
		// Arrange: create 25 blocks (use height as the id)
		// first block starts with transaction id 1
		// second block starts with transaction id 1 + 4 + 2 + 1 + 3 * 3 = 1 + 16 = 17
		// third block starts with transaction id 1 + 2 * 16 = 33 and so on
		// unfortunately hibernate inserts the transaction in alphabetical order of the list names in DbBlock.
		// Thus the ids for the transactions in a block are (x being a non negative multiple of TRANSACTIONS_PER_BLOCK):
		// x + 1:  regular importance transfer 1
		// x + 2:  regular importance transfer 2
		// x + 3:  multisig importance transfer
		// x + 4:  regular modification transaction
		// x + 5:  multisig modification transaction
		// x + 6:  multisig transaction 1
		// x + 7:  multisig transfer transaction
		// x + 8:  multisig signature transaction 1
		// x + 9:  multisig transaction 2
		// x + 10: multisig signature transaction 2
		// x + 11: multisig transaction 3
		// x + 12: multisig signature transaction 3
		// x + 13: regular transfer 1
		// x + 14: regular transfer 2
		// x + 15: regular transfer 3
		// x + 16: regular transfer 4

		if (0 < this.blockDao.count()) {
			return;
		}

		for (int i = 1; i <= 25; i++) {
			final Block block = NisUtils.createRandomBlockWithHeight(2 * i);

			addTransferTransactions(block);
			addImportanceTransferTransactions(block);
			addAggregateModificationTransaction(block);
			addMultigTransactions(block);

			// Arrange: sign and map the blocks
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(accountDao));
			blockDao.save(dbBlock);
		}
	}

	private static void addTransferTransactions(final Block block) {
		// account 0 appears only as sender
		// account 1 appears only as receiver
		// account 2 appears as sender and receiver in different transactions
		// account 3 has one self transaction
		block.addTransaction(createTransfer((int)(block.getHeight().getRaw() * 100), ACCOUNTS[0], ACCOUNTS[1], true));
		block.addTransaction(createTransfer((int)(block.getHeight().getRaw() * 100 + 1), ACCOUNTS[0], ACCOUNTS[2], true));
		block.addTransaction(createTransfer((int)(block.getHeight().getRaw() * 100 + 2), ACCOUNTS[2], ACCOUNTS[1], true));
		block.addTransaction(createTransfer((int)(block.getHeight().getRaw() * 100 + 3), ACCOUNTS[3], ACCOUNTS[3], true));
	}

	private static void addImportanceTransferTransactions(final Block block) {
		// account 0 + 2 appears only as sender
		// account 1 + 3 appears only as remote
		block.addTransaction(createImportanceTransfer((int)(block.getHeight().getRaw() * 100 + 4), ACCOUNTS[0], ACCOUNTS[1], true));
		block.addTransaction(createImportanceTransfer((int)(block.getHeight().getRaw() * 100 + 5), ACCOUNTS[2], ACCOUNTS[3], true));
	}

	private static void addAggregateModificationTransaction(final Block block) {
		// account 0 is sender
		// accounts 1-3 are cosignatories
		block.addTransaction(createAggregateModificationTransaction(
				(int)(block.getHeight().getRaw() * 100 + 6),
				ACCOUNTS[0],
				Arrays.asList(ACCOUNTS[1], ACCOUNTS[2], ACCOUNTS[3]),
				true));
	}

	private static void addMultigTransactions(final Block block) {
		// account 0 is outer transaction sender
		// account 1 is inner transaction sender
		// account 2 is recipient/remote/added cosignatory
		// account 3 is sender of signature transaction
		block.addTransaction(createMultisigTransaction((int)(block.getHeight().getRaw() * 100 + 7), TransactionTypes.TRANSFER));
		block.addTransaction(createMultisigTransaction((int)(block.getHeight().getRaw() * 100 + 8), TransactionTypes.IMPORTANCE_TRANSFER));
		block.addTransaction(createMultisigTransaction((int)(block.getHeight().getRaw() * 100 + 9), TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION));
	}

	private static Transaction createTransfer(
			final int timeStamp,
			final Account sender,
			final Account recipient,
			final boolean signTransaction) {
		final Transaction transaction = new TransferTransaction(
				new TimeInstant(timeStamp),
				sender,
				recipient,
				Amount.fromNem(8),
				null);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createImportanceTransfer(
			final int timeStamp,
			final Account sender,
			final Account remote,
			final boolean signTransaction) {
		final Transaction transaction = new ImportanceTransferTransaction(
				new TimeInstant(timeStamp),
				sender,
				ImportanceTransferTransaction.Mode.Activate,
				remote);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createAggregateModificationTransaction(
			final int timeStamp,
			final Account sender,
			final Collection<Account> cosignatories,
			final boolean signTransaction) {
		final List<MultisigModification> modifications = cosignatories.stream()
				.map(c -> createModification(MultisigModificationType.Add, c))
				.collect(Collectors.toList());
		final Transaction transaction = new MultisigAggregateModificationTransaction(
				new TimeInstant(timeStamp),
				sender,
				modifications);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createMultisigTransaction(
			final int timeStamp,
			final int innerType) {
		Transaction innerTransaction;
		switch (innerType) {
			case TransactionTypes.TRANSFER:
				innerTransaction = createTransfer(timeStamp, ACCOUNTS[1], ACCOUNTS[2], false);
				break;
			case TransactionTypes.IMPORTANCE_TRANSFER:
				innerTransaction = createImportanceTransfer(timeStamp, ACCOUNTS[1], ACCOUNTS[2], false);
				break;
			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				innerTransaction = createAggregateModificationTransaction(timeStamp, ACCOUNTS[1], Arrays.asList(ACCOUNTS[2]), false);
				break;
			default:
				throw new RuntimeException("invalid inner transaction type.");
		}
		final Hash hash = HashUtils.calculateHash(innerTransaction);

		final MultisigTransaction multisig = new MultisigTransaction(
				new TimeInstant(timeStamp),
				ACCOUNTS[0],
				innerTransaction);
		multisig.addSignature(createSignature(timeStamp, ACCOUNTS[3], ACCOUNTS[1], hash));
		multisig.sign();
		return multisig;
	}

	private static MultisigSignatureTransaction createSignature(
			final int timeStamp,
			final Account cosigner,
			final Account multisig,
			final Hash hash) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(timeStamp),
				cosigner,
				multisig,
				hash);
		transaction.sign();
		return transaction;
	}

	private static MultisigModification createModification(final MultisigModificationType type, final Account account) {
		return new MultisigModification(type, account);
	}

	protected Long getAccountId(final Account account) {
		final Address address = account.getAddress();
		final Query query = this.session
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}
}
