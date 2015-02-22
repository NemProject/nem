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
import java.util.function.BiFunction;
import java.util.stream.*;

// TODO 20150220 J-B: i like how you refactored the tests; good job :)

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class TransactionRetrieverTest {
	private static final long TRANSACTIONS_PER_BLOCK = 16L;
	private static final int LIMIT = 10;
	private static final MockAccountDao ACCOUNT_DAO = new MockAccountDao();
	private static final Account[] ACCOUNTS = {
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount(),
			Utils.generateRandomAccount()
	};

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void beforeClass() {
		this.session = this.sessionFactory.openSession();
		this.setupBlocks();
	}

	@After
	public void afterClass() {
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
	 * Gets the list of expected comparable pairs for incoming transfers.
	 *
	 * @param height The block height.
	 * @param accountIndex The account index in the array.
	 * @return The expected pairs.
	 */
	protected abstract List<ComparablePair> getExpectedComparablePairsForIncomingTransactions(final BlockHeight height, final int accountIndex);

	/**
	 * Gets the list of expected comparable pairs for outgoing transfers.
	 *
	 * @param height The block height.
	 * @param accountIndex The account index in the array.
	 * @return The expected pairs.
	 */
	protected abstract List<ComparablePair> getExpectedComparablePairsForOutgoingTransactions(final BlockHeight height, final int accountIndex);

	@Test
	public void canRetrieveIncomingTransactions() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, 33));
	}

	@Test
	public void canRetrieveOutgoingTransactions() {
		IntStream.range(0, 4).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, 33));
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
		final Collection<ComparablePair> comparablePairs = pairs.stream()
				.map(ComparablePair::new)
				.collect(Collectors.toList());

		// Assert:
		final Collection<ComparablePair> expectedComparablePairs = getExpectedComparablePairsForTransactions(
				this::getExpectedComparablePairsForIncomingTransactions,
				accountIndex,
				topMostId,
				LIMIT) ;
		Assert.assertThat(comparablePairs, IsEquivalent.equivalentTo(expectedComparablePairs));
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
		final Collection<ComparablePair> comparablePairs = pairs.stream()
				.map(ComparablePair::new)
				.collect(Collectors.toList());

		// Assert:
		final Collection<ComparablePair> expectedComparablePairs = getExpectedComparablePairsForTransactions(
				this::getExpectedComparablePairsForOutgoingTransactions,
				accountIndex,
				topMostId,
				LIMIT) ;
		Assert.assertThat(comparablePairs, IsEquivalent.equivalentTo(expectedComparablePairs));
	}

	private List<ComparablePair> getExpectedComparablePairsForTransactions(
			final BiFunction<BlockHeight, Integer, List<ComparablePair>> expectedComparablePairsSupplier,
			final int accountIndex,
			final long topMostTransactionId,
			final int limit) {
		final List<ComparablePair> pairs = new ArrayList<>();
		long height = Math.min(Math.max(((topMostTransactionId - 2) / TRANSACTIONS_PER_BLOCK + 1) * 2, 1), 50);
		while (limit > pairs.size() && height > 0) {
			pairs.addAll(expectedComparablePairsSupplier.apply(new BlockHeight(height), accountIndex));
			height -= 2L;
		}

		return pairs.subList(0, limit > pairs.size() ? pairs.size() : limit);
	}

	protected void setupBlocks() {
		// Arrange: create 25 blocks (use height as the id)
		// first block starts with transaction id 1
		// second block starts with transaction id 1 + 4 + 2 + 1 + 3 * 3 = 1 + 16 = 17
		// third block starts with transaction id 1 + 2 * 16 = 33 and so on
		for (int i = 1; i <= 25; i++) {
			final Block block = NisUtils.createRandomBlockWithHeight(2 * i);

			addTransferTransactions(block);
			addImportanceTransferTransactions(block);
			addAggregateModificationTransaction(block);
			addMultigTransactions(block);

			// Arrange: sign and map the blocks
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(ACCOUNT_DAO));
			this.blockDao.save(dbBlock);
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

	private static void addMapping(final MockAccountDao accountDao, final Account account) {
		accountDao.addMapping(account, new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey()));
	}

	private static Transaction createTransfer(
			final int timeStamp,
			final Account sender,
			final Account recipient,
			final boolean signTransaction) {
		addMapping(ACCOUNT_DAO, sender);
		addMapping(ACCOUNT_DAO, recipient);
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
		addMapping(ACCOUNT_DAO, sender);
		addMapping(ACCOUNT_DAO, remote);
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
		addMapping(ACCOUNT_DAO, sender);
		modifications.forEach(m -> addMapping(ACCOUNT_DAO, m.getCosignatory()));
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

	protected class ComparablePair {
		public final long blockHeight;
		public final int transactionTimeStamp;

		public ComparablePair(final long blockHeight, final int transactionTimeStamp) {
			this.blockHeight = blockHeight;
			this.transactionTimeStamp = transactionTimeStamp;
		}

		public ComparablePair(final TransferBlockPair pair) {
			this.blockHeight = pair.getDbBlock().getHeight();
			this.transactionTimeStamp = pair.getTransfer().getTimeStamp();
		}

		@Override
		public int hashCode() {
			return Long.valueOf(this.blockHeight).hashCode() ^ this.transactionTimeStamp;
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof ComparablePair)) {
				return false;
			}

			final ComparablePair rhs = (ComparablePair)obj;
			return this.blockHeight == rhs.blockHeight && this.transactionTimeStamp == rhs.transactionTimeStamp;
		}

		@Override
		public String toString() {
			return String.format("B: %s; T: %d", this.blockHeight, this.transactionTimeStamp);
		}
	}
}
