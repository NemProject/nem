package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferRetrieverTest {

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
		session.createSQLQuery("delete from transfers").executeUpdate();
		session.createSQLQuery("delete from importancetransfers").executeUpdate();
		session.createSQLQuery("delete from multisigmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsignatures").executeUpdate();
		session.createSQLQuery("delete from multisigsignermodifications").executeUpdate();
		session.createSQLQuery("delete from multisigtransactions").executeUpdate();
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
	}

	@After
	public void after() {
		this.session.close();
	}

	private final Account ACCOUNT1 = Utils.generateRandomAccount();
	private final Account ACCOUNT2 = Utils.generateRandomAccount();
	private final Account ACCOUNT3 = Utils.generateRandomAccount();
	private final Account ACCOUNT4 = Utils.generateRandomAccount();

	@Test
	public void canRetrieveIncomingTransactions() {
		// Arrange:
		this.setupBlocks();
		final TransactionRetriever retriever = new TransferRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(
				this.session,
				this.getAccountId(ACCOUNT1),
				10,
				10,
				ReadOnlyTransferDao.TransferType.INCOMING);
		final Collection<ComparablePair> comparablePairs = pairs.stream()
				.map(p -> new ComparablePair(p))
				.collect(Collectors.toList());

		// Assert:
		final Collection<ComparablePair> expectedComparablePairs = Arrays.asList(
				new ComparablePair(8, 8),
				new ComparablePair(2, 1));
		Assert.assertThat(comparablePairs, IsEqual.equalTo(expectedComparablePairs));
	}

	@Test
	public void canRetrieveOutgoingTransactions() {
		// Arrange:
		this.setupBlocks();
		final TransactionRetriever retriever = new TransferRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(
				this.session,
				this.getAccountId(ACCOUNT1),
				10,
				10,
				ReadOnlyTransferDao.TransferType.OUTGOING);
		final Collection<ComparablePair> comparablePairs = pairs.stream()
				.map(p -> new ComparablePair(p))
				.collect(Collectors.toList());

		// Assert:
		final Collection<ComparablePair> expectedComparablePairs = Arrays.asList(
				new ComparablePair(4, 7));
		Assert.assertThat(comparablePairs, IsEqual.equalTo(expectedComparablePairs));
	}

	private Long getAccountId(final Account account) {
		final Address address = account.getAddress();
		final Query query = this.session
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}

	// TODO: add more tests

	private void setupBlocks() {
		// Arrange: create three blocks (use height as the id)
		final Block block1 = NisUtils.createRandomBlockWithHeight(2);
		final Block block2 = NisUtils.createRandomBlockWithHeight(4);
		final Block block3 = NisUtils.createRandomBlockWithHeight(8);

		// Arrange: setup the accounts (ids are in order 1 - 4)
		final MockAccountDao accountDao = new MockAccountDao();
		addMapping(accountDao, ACCOUNT1);
		addMapping(accountDao, ACCOUNT2);
		addMapping(accountDao, ACCOUNT3);
		addMapping(accountDao, ACCOUNT4);

		// Arrange: setup 8 transfers (use timestamp as the id)
		block1.addTransaction(createTransfer(1, ACCOUNT3, ACCOUNT1));
		block1.addTransaction(createTransfer(2, ACCOUNT3, ACCOUNT2));
		block2.addTransaction(createTransfer(3, ACCOUNT4, ACCOUNT4));
		block2.addTransaction(createTransfer(4, ACCOUNT4, ACCOUNT3));
		block2.addTransaction(createTransfer(5, ACCOUNT2, ACCOUNT3));
		block2.addTransaction(createTransfer(6, ACCOUNT2, ACCOUNT2));
		block2.addTransaction(createTransfer(7, ACCOUNT1, ACCOUNT2));
		block3.addTransaction(createTransfer(8, ACCOUNT2, ACCOUNT1));

		// Arrange: sign and map the blocks
		for (final Block block : Arrays.asList(block1, block2, block3)) {
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(accountDao));
			this.blockDao.save(dbBlock);
		}
	}

	private static void addMapping(final MockAccountDao accountDao, final Account account) {
		accountDao.addMapping(account, new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey()));
	}

	private static Transaction createTransfer(final int timeStamp, final Account sender, final Account recipient) {
		final Transaction transfer = new TransferTransaction(
				new TimeInstant(timeStamp),
				sender,
				recipient,
				Amount.fromNem(8),
				null);
		transfer.sign();
		return transfer;
	}

	private class ComparablePair {
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