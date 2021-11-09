package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("rawtypes")
public class TransferDaoTest {
	private static final int USE_HASH = 1;
	private static final int USE_ID = 2;
	private static final int DEFAULT_LIMIT = 25;

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	SynchronizedAccountStateCache accountStateCache;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbTestUtils.dbCleanup(this.session);
		DbTestUtils.cacheCleanup(this.accountStateCache);
		this.session.close();
	}

	// region getTransactionUsingHash

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransferBlockPair() {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, 30);

		// Act:
		for (int i = 0; i < context.hashes.size(); ++i) {
			final TransferBlockPair pair = this.transferDao.getTransactionUsingHash(context.hashes.get(i), BlockHeight.ONE);
			MatcherAssert.assertThat(2 * i, IsEqual.equalTo(pair.getTransfer().getTimeStamp()));
			MatcherAssert.assertThat(context.hashes.get(i), IsEqual.equalTo(pair.getTransfer().getTransferHash()));
			MatcherAssert.assertThat(context.account.getAddress().toString(),
					IsEqual.equalTo(pair.getTransfer().getSender().getPrintableKey()));
			MatcherAssert.assertThat(123, IsEqual.equalTo(pair.getDbBlock().getTimeStamp()));
			MatcherAssert.assertThat(1L, IsEqual.equalTo(pair.getDbBlock().getHeight()));
			MatcherAssert.assertThat(context.account.getAddress().toString(),
					IsEqual.equalTo(pair.getDbBlock().getHarvester().getPrintableKey()));
		}
	}

	// endregion

	// region getTransactionsForAccountUsingHash

	@Test
	public void getTransactionsForAccountUsingHashRespectsHash() {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, 30);

		// Act
		final Collection<AbstractBlockTransfer> entities1 = this.getTransfersFromDbUsingAttribute(context, null, null, USE_HASH);
		final Collection<AbstractBlockTransfer> entities2 = this.getTransfersFromDbUsingAttribute(context, context.hashes.get(5), null,
				USE_HASH);
		final Collection<AbstractBlockTransfer> entities3 = this.getTransfersFromDbUsingAttribute(context, context.hashes.get(0), null,
				USE_HASH);

		// Assert:
		MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
		MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(entities3.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getTransactionsForAccountUsingHashThrowsWhenHashNotFound() {
		// Arrange:
		this.assertGetTransactionsForAccountUsingAttributeThrowsWhenAttributeNotFound(USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingIncomingTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingIncomingTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingIncomingTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingAllTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(ReadOnlyTransferDao.TransferType.ALL,
				o -> 99 - o, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingAllTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.ALL, o -> 99 - o, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsCorrectTransfersWhenQueryingAllTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(ReadOnlyTransferDao.TransferType.ALL,
				o -> 99 - o, USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsSortedResults() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeReturnsResultsSortedById(USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashFiltersDuplicatesIfTransferTypeIsAll() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeFiltersDuplicatesIfTransferTypeIsAll(USE_HASH);
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsEmptyCollectionIfSenderIsUnknown() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeReturnsEmptyCollectionIfSenderIsUnknown(USE_HASH);
	}

	// endregion

	// region getTransactionsForAccountUsingId

	@Test
	public void getTransactionsForAccountUsingIdRespectsId() {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, 30);

		// Act
		final Collection<?> entities1 = this.getTransfersFromDbUsingAttribute(context, null, null, USE_ID);
		final Collection<?> entities2 = this.getTransfersFromDbUsingAttribute(context, null, 6L, USE_ID);
		final Collection<?> entities3 = this.getTransfersFromDbUsingAttribute(context, null, 1L, USE_ID);

		// Assert:
		MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
		MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(entities3.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getTransactionsForAccountUsingIdSucceedsWhenIdNotFound() {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, 30);

		// Act
		final Collection<?> entities = this.getTransfersFromDbUsingAttribute(context, null, 1234L, USE_ID);

		// Assert:
		MatcherAssert.assertThat(entities.size(), IsEqual.equalTo(25));
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingIncomingTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingIncomingTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingIncomingTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(
				ReadOnlyTransferDao.TransferType.INCOMING, o -> 2 * (49 - o) + 1, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingOutgoingTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(
				ReadOnlyTransferDao.TransferType.OUTGOING, o -> 2 * (49 - o), USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingAllTransfersFromStart() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(ReadOnlyTransferDao.TransferType.ALL,
				o -> 99 - o, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingAllTransfersFromMiddle() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
				ReadOnlyTransferDao.TransferType.ALL, o -> 99 - o, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsCorrectTransfersWhenQueryingAllTransfersFromEnd() {
		this.assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(ReadOnlyTransferDao.TransferType.ALL,
				o -> 99 - o, USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsSortedResults() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeReturnsResultsSortedById(USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdFiltersDuplicatesIfTransferTypeIsAll() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeFiltersDuplicatesIfTransferTypeIsAll(USE_ID);
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsEmptyCollectionIfSenderIsUnknown() {
		// Assert:
		this.assertGetTransactionsForAccountUsingAttributeReturnsEmptyCollectionIfSenderIsUnknown(USE_ID);
	}

	// endregion

	private class TestContext {
		private final BlockDao blockDao;
		private final ReadOnlyTransferDao.TransferType transferType;
		private Account account;
		private final BlockHeight height;
		private final List<Hash> hashes = new ArrayList<>();

		public TestContext(final BlockDao blockDao, final ReadOnlyTransferDao.TransferType transferType) {
			this.blockDao = blockDao;
			this.transferType = transferType;
			this.height = BlockHeight.ONE;
			this.prepareBlockWithIncomingAndOutgoingTransactions();
		}

		public TestContext(final BlockDao blockDao, final int count) {
			this.blockDao = blockDao;
			this.transferType = ReadOnlyTransferDao.TransferType.OUTGOING;
			this.height = BlockHeight.ONE;
			this.prepareBlockWithOutgoingTransactions(count);
		}

		private void prepareBlockWithIncomingAndOutgoingTransactions() {
			this.account = Utils.generateRandomAccount();
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			TransferDaoTest.this.addMapping(mockAccountDao, this.account);
			final Block dummyBlock = new Block(this.account, Hash.ZERO, Hash.ZERO, new TimeInstant(123), this.height);
			TransferTransaction transferTransaction;

			for (int i = 0; i < 100; i++) {
				final Account otherAccount = Utils.generateRandomAccount();
				TransferDaoTest.this.addMapping(mockAccountDao, otherAccount);
				if (i % 2 == 0) {
					transferTransaction = TransferDaoTest.this.prepareTransferTransaction(this.account, otherAccount, 10, i);
					dummyBlock.addTransaction(transferTransaction);
				} else {
					transferTransaction = TransferDaoTest.this.prepareTransferTransaction(otherAccount, this.account, 10, i);
					dummyBlock.addTransaction(transferTransaction);
				}
				this.hashes.add(HashUtils.calculateHash(transferTransaction));
			}
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		private void prepareBlockWithOutgoingTransactions(final int count) {
			this.account = Utils.generateRandomAccount();
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			TransferDaoTest.this.addMapping(mockAccountDao, this.account);
			final Block dummyBlock = new Block(this.account, Hash.ZERO, Hash.ZERO, new TimeInstant(123), this.height);

			for (int i = 0; i < count; i++) {
				final Account recipient = Utils.generateRandomAccount();
				TransferDaoTest.this.addMapping(mockAccountDao, recipient);
				final TransferTransaction transferTransaction = TransferDaoTest.this.prepareTransferTransaction(this.account, recipient, 10,
						2 * i);

				dummyBlock.addTransaction(transferTransaction);
				this.hashes.add(HashUtils.calculateHash(transferTransaction));
			}
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		private List<Integer> getTestIntegerList(final Function<Integer, Integer> mapper) {
			return IntStream.range(0, 50).map(mapper::apply).boxed().collect(Collectors.toList());
		}
	}

	// region multisig transactions

	@Test
	public void getTransactionsForAccountUsingIdReturnsExpectedOutgoingMultisigTransactions() {
		// Arrange:
		final MultisigTestContext context = this.createMultisigTestContext();
		final List<MultisigTransaction> expectedTransactions = Arrays.asList(context.multisigTransferTransaction,
				context.multisigImportanceTransferTransaction, context.multisigAggregateModificationTransaction);

		// Assert:
		// 1) signer of multisig transaction sees his own transactions as outgoing
		this.assertExpectedMultisigTransactions(context.multisigSigner, ReadOnlyTransferDao.TransferType.OUTGOING, expectedTransactions);

		// 2) multisig account sees the multisig transactions as outgoing (because inner transaction involves the multisig account)
		this.assertExpectedMultisigTransactions(context.multisig, ReadOnlyTransferDao.TransferType.OUTGOING, expectedTransactions);

		// 3) cosignatories of the multisig transaction see the transactions as outgoing
		context.cosignatories.stream()
				.forEach(c -> this.assertExpectedMultisigTransactions(c, ReadOnlyTransferDao.TransferType.OUTGOING, expectedTransactions));

		// 4) recipient (if any) of the inner transaction does not see the transaction as outgoing
		this.assertExpectedMultisigTransactions(context.recipient, ReadOnlyTransferDao.TransferType.OUTGOING, new ArrayList<>());

		// 5) other accounts do not see any transaction as outgoing
		this.assertExpectedMultisigTransactions(Utils.generateRandomAccount(), ReadOnlyTransferDao.TransferType.OUTGOING,
				new ArrayList<>());
	}

	@Test
	public void getTransactionsForAccountUsingIdReturnsExpectedIncomingMultisigTransactions() {
		// Arrange:
		final MultisigTestContext context = this.createMultisigTestContext();

		// Assert:
		// 1) signer of multisig transaction does not any the transaction as incoming
		this.assertExpectedMultisigTransactions(context.multisigSigner, ReadOnlyTransferDao.TransferType.INCOMING, new ArrayList<>());

		// 2) multisig account does not see any transaction as incoming
		this.assertExpectedMultisigTransactions(context.multisig, ReadOnlyTransferDao.TransferType.INCOMING, new ArrayList<>());

		// 3) cosignatories of the multisig transaction do not see any transaction as incoming
		context.cosignatories.stream()
				.forEach(c -> this.assertExpectedMultisigTransactions(c, ReadOnlyTransferDao.TransferType.INCOMING, new ArrayList<>()));

		// 4) recipient (if any) of inner transaction sees the transaction as incoming
		this.assertExpectedMultisigTransactions(context.recipient, ReadOnlyTransferDao.TransferType.INCOMING,
				Arrays.asList(context.multisigTransferTransaction, context.multisigImportanceTransferTransaction));

		// 5) the new cosignatory of the inner transaction (in case of modification) sees the transaction as incoming
		this.assertExpectedMultisigTransactions(context.newCosignatory, ReadOnlyTransferDao.TransferType.INCOMING,
				Collections.singletonList(context.multisigAggregateModificationTransaction));

		// 6) other accounts do not see any transaction as incoming
		this.assertExpectedMultisigTransactions(Utils.generateRandomAccount(), ReadOnlyTransferDao.TransferType.INCOMING,
				new ArrayList<>());
	}

	private MultisigTestContext createMultisigTestContext() {
		final ExtendedAccountStateCache cache = this.accountStateCache.copy();
		final MultisigTestContext context = new MultisigTestContext(this.blockDao, cache);
		cache.commit();
		context.prepareBlockWithMultisigTransactions();
		return context;
	}

	private void assertExpectedMultisigTransactions(final Account account, final ReadOnlyTransferDao.TransferType transferType,
			final Collection<MultisigTransaction> multisigTransactions) {

		// Act:
		final Collection<Hash> transactions = this.transferDao.getTransactionsForAccountUsingId(account, null, transferType, DEFAULT_LIMIT)
				.stream().map(p -> p.getTransfer().getTransferHash()).collect(Collectors.toList());
		final Collection<Hash> expectedTransactions = multisigTransactions.stream().map(HashUtils::calculateHash)
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(transactions, IsEquivalent.equivalentTo(expectedTransactions));
	}

	private class MultisigTestContext {
		private final BlockDao blockDao;
		private final Account harvester;
		private final Account multisigSigner;
		private final Account multisig;
		private final Account recipient;
		private final List<Account> cosignatories;
		private final Account newCosignatory;
		private final Collection<Transaction> transactions;
		private MultisigTransaction multisigTransferTransaction;
		private MultisigTransaction multisigImportanceTransferTransaction;
		private MultisigTransaction multisigAggregateModificationTransaction;

		private MultisigTestContext(final BlockDao blockDao, final AccountStateCache accountStateCache) {
			this.blockDao = blockDao;
			this.multisigSigner = Utils.generateRandomAccount();
			this.harvester = Utils.generateRandomAccount();
			this.multisig = Utils.generateRandomAccount();
			this.recipient = Utils.generateRandomAccount();
			this.cosignatories = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount());
			this.newCosignatory = Utils.generateRandomAccount();
			this.transactions = new ArrayList<>();
			final AccountState state = accountStateCache.findStateByAddress(this.multisig.getAddress());
			this.cosignatories.stream().map(Account::getAddress).forEach(a -> state.getMultisigLinks().addCosignatory(a));
			state.getMultisigLinks().addCosignatory(this.multisigSigner.getAddress());
		}

		private void prepareBlockWithMultisigTransactions() {
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			TransferDaoTest.this.addMapping(mockAccountDao, this.harvester);
			TransferDaoTest.this.addMapping(mockAccountDao, this.multisigSigner);
			TransferDaoTest.this.addMapping(mockAccountDao, this.multisig);
			TransferDaoTest.this.addMapping(mockAccountDao, this.recipient);
			this.cosignatories.forEach(c -> TransferDaoTest.this.addMapping(mockAccountDao, c));
			final Block block = new Block(this.harvester, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

			// multisig transfer transactions
			this.addMultisigTransferTransactions();

			// multisig importance transfer transactions
			this.addMultisigImportanceTransferTransactions();

			// multisig aggregate modification transactions
			this.addMultisigAggregateModificationTransactions();

			block.addTransactions(this.transactions);
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		private void addMultisigTransferTransactions() {
			this.multisigTransferTransaction = this.prepareMultisigTransaction(
					this.createTransferTransaction(this.multisig, this.recipient), this.multisigSigner, this.cosignatories);
			this.transactions.add(this.multisigTransferTransaction);
			for (int i = 0; i < 3; i++) {
				final MultisigTransaction tx = this.prepareMultisigTransaction(
						this.createTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount()),
						Utils.generateRandomAccount(),
						Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount()));
				this.transactions.add(tx);
			}
		}

		private void addMultisigImportanceTransferTransactions() {
			this.multisigImportanceTransferTransaction = this.prepareMultisigTransaction(
					this.createImportanceTransferTransaction(this.multisig, this.recipient), this.multisigSigner, this.cosignatories);
			this.transactions.add(this.multisigImportanceTransferTransaction);
			for (int i = 0; i < 3; i++) {
				final MultisigTransaction tx = this.prepareMultisigTransaction(
						this.createImportanceTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount()),
						Utils.generateRandomAccount(),
						Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount()));
				this.transactions.add(tx);
			}
		}

		private void addMultisigAggregateModificationTransactions() {
			this.multisigAggregateModificationTransaction = this.prepareMultisigTransaction(
					this.createMultisigAggregateModificationTransaction(this.multisig, this.newCosignatory), this.multisigSigner,
					this.cosignatories);
			this.transactions.add(this.multisigAggregateModificationTransaction);
			for (int i = 0; i < 3; i++) {
				final MultisigTransaction tx = this.prepareMultisigTransaction(
						this.createMultisigAggregateModificationTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount()),
						Utils.generateRandomAccount(),
						Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount()));
				this.transactions.add(tx);
			}
		}

		private MultisigTransaction prepareMultisigTransaction(final Transaction otherTransaction, final Account multisigSigner,
				final List<Account> cosignatories) {
			final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, multisigSigner, otherTransaction);
			cosignatories.forEach(c -> this.addSignature(c, transaction));
			transaction.sign();

			return transaction;
		}

		private TransferTransaction createTransferTransaction(final Account transferSender, final Account transferRecipient) {
			return new TransferTransaction(TimeInstant.ZERO, transferSender, transferRecipient, Amount.fromNem(123), null);
		}

		private ImportanceTransferTransaction createImportanceTransferTransaction(final Account transferSender,
				final Account transferRecipient) {
			return new ImportanceTransferTransaction(TimeInstant.ZERO, transferSender, ImportanceTransferMode.Activate, transferRecipient);
		}

		private MultisigAggregateModificationTransaction createMultisigAggregateModificationTransaction(final Account multisig,
				final Account newCosignatory) {
			final List<MultisigCosignatoryModification> modifications = Collections
					.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, newCosignatory));
			return new MultisigAggregateModificationTransaction(TimeInstant.ZERO, multisig, modifications);
		}

		public void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
			final Transaction otherTransaction = multisigTransaction.getOtherTransaction();
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(TimeInstant.ZERO, signatureSigner,
					otherTransaction.getSigner(), otherTransaction);
			signatureTransaction.sign();
			multisigTransaction.addSignature(signatureTransaction);
		}
	}

	// endregion

	private Collection<AbstractBlockTransfer> getTransfersFromDbUsingAttribute(final TestContext context, final Hash hash, final Long id,
			final int callType) {
		return this
				.executeGetTransactionsForAccountUsingAttribute(context.account, hash, id, context.height, context.transferType, callType)
				.stream().map(TransferBlockPair::getTransfer).collect(Collectors.toList());
	}

	private void assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromStart(
			final ReadOnlyTransferDao.TransferType transferType, final Function<Integer, Integer> mapper, final int callType) {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, transferType);
		final List<Integer> expectedTimeStamps = context.getTestIntegerList(mapper);
		final List<Integer> timeStamps = this.getTransfersFromDbUsingAttribute(context, null, null, callType).stream()
				.map(AbstractBlockTransfer::getTimeStamp).collect(Collectors.toList());

		// Assert
		MatcherAssert.assertThat(timeStamps.size(), IsEqual.equalTo(25));
		MatcherAssert.assertThat(timeStamps, IsEqual.equalTo(expectedTimeStamps.subList(0, 25)));
	}

	private void assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromMiddle(
			final ReadOnlyTransferDao.TransferType transferType, final Function<Integer, Integer> mapper, final int callType) {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, transferType);
		final List<Integer> expectedTimeStamps = context.getTestIntegerList(mapper);
		final List<Integer> timeStamps = this
				.getTransfersFromDbUsingAttribute(context, USE_HASH == callType ? context.hashes.get(mapper.apply(24)) : null,
						USE_ID == callType ? (long) mapper.apply(24) + 1 : null, callType)
				.stream().map(AbstractBlockTransfer::getTimeStamp).collect(Collectors.toList());

		// Assert
		MatcherAssert.assertThat(timeStamps.size(), IsEqual.equalTo(25));
		MatcherAssert.assertThat(timeStamps, IsEqual.equalTo(expectedTimeStamps.subList(25, 50)));
	}

	private void assertGetTransactionsForAccountUsingAttributeReturnsCorrectTransfersWhenQueryingFromEnd(
			final ReadOnlyTransferDao.TransferType transferType, final Function<Integer, Integer> mapper, final int callType) {
		// Arrange:
		final TestContext context = new TestContext(this.blockDao, transferType);
		final int pos = ReadOnlyTransferDao.TransferType.ALL == transferType ? 99 : 49;
		final List<Integer> timeStamps = this
				.getTransfersFromDbUsingAttribute(context, USE_HASH == callType ? context.hashes.get(mapper.apply(pos)) : null,
						USE_ID == callType ? (long) mapper.apply(pos) + 1 : null, callType)
				.stream().map(AbstractBlockTransfer::getTimeStamp).collect(Collectors.toList());

		// Assert
		MatcherAssert.assertThat(timeStamps.size(), IsEqual.equalTo(0));
	}

	private void assertGetTransactionsForAccountUsingAttributeThrowsWhenAttributeNotFound(final int callType) {
		// Arrange: roundabout way to ensure that account is added to the db under test
		final Account account = Utils.generateRandomAccount();
		final long heights[] = {
				3
		};
		final int blockTimestamp[] = {
				1801
		};
		final int txTimestamps[][] = {
				{
						1800
				}
		};
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, account, true);

		// Act:
		ExceptionAssert
				.assertThrows(
						v -> this.executeGetTransactionsForAccountUsingAttribute(account, Utils.generateRandomHash(),
								new SecureRandom().nextLong(), BlockHeight.ONE, ReadOnlyTransferDao.TransferType.INCOMING, callType),
						MissingResourceException.class);
	}

	private void assertGetTransactionsForAccountUsingAttributeReturnsResultsSortedById(final int type) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();

		final List<Long> expectedIds = Arrays.asList(9l, 8l, 7l, 6l, 5l, 4l, 3l, 2l, 1l);
		final long heights[] = {
				3, 4, 1, 2
		};
		final int blockTimestamp[] = {
				1801, 1901, 1401, 1501
		};
		final int txTimestamps[][] = {
				{
						1800, 1600
				}, {
						1900, 1700, 1700
				}, {
						1200, 1400
				}, {
						1300, 1500
				}
		};
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, sender, false);

		// Act
		final Collection<TransferBlockPair> entities1 = this.executeGetTransactionsForAccountUsingAttribute(sender, null, null,
				BlockHeight.ONE, ReadOnlyTransferDao.TransferType.ALL, type);

		final List<Long> resultIds = entities1.stream().map(pair -> pair.getTransfer().getId()).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(9));
		MatcherAssert.assertThat(resultIds, IsEqual.equalTo(expectedIds));
	}

	private void assertGetTransactionsForAccountUsingAttributeFiltersDuplicatesIfTransferTypeIsAll(final int type) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final long heights[] = {
				3
		};
		final int blockTimestamp[] = {
				1801
		};
		final int txTimestamps[][] = {
				{
						1800
				}
		};
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, sender, true);
		final Collection<TransferBlockPair> entities = this.executeGetTransactionsForAccountUsingAttribute(sender, null, null,
				BlockHeight.ONE, ReadOnlyTransferDao.TransferType.ALL, type);

		// Assert:
		MatcherAssert.assertThat(entities.size(), IsEqual.equalTo(1));
	}

	private void assertGetTransactionsForAccountUsingAttributeReturnsEmptyCollectionIfSenderIsUnknown(final int type) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final long heights[] = {
				3, 4, 1, 2
		};
		final int blockTimestamp[] = {
				1801, 1901, 1401, 1501
		};
		final int txTimestamps[][] = {
				{
						1800, 1600
				}, {
						1900, 1700, 1700
				}, {
						1200, 1400
				}, {
						1300, 1500
				}
		};
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, sender, false);

		// Act:
		final Collection<TransferBlockPair> entities = this.executeGetTransactionsForAccountUsingAttribute(Utils.generateRandomAccount(),
				null, null, BlockHeight.ONE, ReadOnlyTransferDao.TransferType.ALL, type);

		// Assert:
		MatcherAssert.assertThat(entities.isEmpty(), IsEqual.equalTo(true));
	}

	private Collection<TransferBlockPair> executeGetTransactionsForAccountUsingAttribute(final Account sender, final Hash hash,
			final Long id, final BlockHeight height, final ReadOnlyTransferDao.TransferType transferType, final int type) {
		switch (type) {
			case USE_HASH:
				return this.transferDao.getTransactionsForAccountUsingHash(sender, hash, height, transferType, DEFAULT_LIMIT);
			case USE_ID:
				return this.transferDao.getTransactionsForAccountUsingId(sender, id, transferType, DEFAULT_LIMIT);
			default :
				throw new IllegalArgumentException("unknown call type");
		}
	}

	private void createTestBlocks(final long[] heights, final int[] blockTimestamp, final int[][] txTimestamps, final Account sender,
			final boolean useSenderAsRecipient) {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		this.addMapping(mockAccountDao, sender);

		for (int i = 0; i < heights.length; ++i) {
			final long height = heights[i];
			final int blockTs = blockTimestamp[i];

			final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(blockTs), new BlockHeight(height));

			for (final int txTimestamp : txTimestamps[i]) {
				final Account recipient = useSenderAsRecipient ? sender : Utils.generateRandomAccount();
				this.addMapping(mockAccountDao, recipient);
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, txTimestamp);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount, final int i) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(new TimeInstant(i), sender, recipient,
				Amount.fromNem(amount), null);
		transferTransaction.sign();
		return transferTransaction;
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final DbAccount dbSender = new DbAccount(account.getAddress());
		mockAccountDao.addMapping(account, dbSender);
	}
}
