package org.nem.nis.dao.retrievers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.*;

/**
 * Note that these tests are retrieving transactions by providing a topmost id, which means they depend on the order in which hibernate
 * saves the transactions to the db
 */
@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class TransactionRetrieverTest {
	protected static final long TRANSACTIONS_PER_BLOCK = 34L;
	private static final int LIMIT = 10;
	protected static final Account[] ACCOUNTS = {
			Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount(),
			Utils.generateRandomAccount()
	};

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private SynchronizedAccountStateCache accountStateCache;

	@Autowired
	protected MosaicIdCache mosaicIdCache;

	protected Session session;

	@Before
	public void setup() {
		Utils.setupGlobals();
		this.session = this.sessionFactory.openSession();
		this.setupBlocks();
	}

	@After
	public void destroy() {
		if (null != this.session) {
			DbTestUtils.dbCleanup(this.session);
			DbTestUtils.cacheCleanup(this.accountStateCache);
			this.session.close();
		}

		this.mosaicIdCache.clear();
		Utils.resetGlobals();
	}

	/**
	 * Gets the transaction retriever. Must be implemented by subclass.
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

	// region retrieval

	@Test
	public void canRetrieveIncomingTransactionsFromBeginning() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, 36));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromBeginning() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, 36));
	}

	@Test
	public void canRetrieveIncomingTransactionsFromMiddle() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, 234));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromMiddle() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, 234));
	}

	@Test
	public void canRetrieveIncomingTransactionsFromEnd() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveIncomingTransactions(i, Long.MAX_VALUE));
	}

	@Test
	public void canRetrieveOutgoingTransactionsFromEnd() {
		IntStream.range(0, ACCOUNTS.length).forEach(i -> this.assertCanRetrieveOutgoingTransactions(i, Long.MAX_VALUE));
	}

	private void assertCanRetrieveIncomingTransactions(final int accountIndex, final long topMostId) {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session,
				this.getAccountId(ACCOUNTS[accountIndex]), topMostId, LIMIT, ReadOnlyTransferDao.TransferType.INCOMING);
		final Collection<Integer> ids = pairs.stream().map(p -> (int) p.getTransfer().getId().longValue()).collect(Collectors.toList());

		// Assert:
		final Collection<Integer> expectedIds = this.getExpectedIdsForTransactions(this::getExpectedComparablePairsForIncomingTransactions,
				accountIndex, topMostId, LIMIT);
		MatcherAssert.assertThat(ids, IsEquivalent.equivalentTo(expectedIds));
	}

	private void assertCanRetrieveOutgoingTransactions(final int accountIndex, final long topMostId) {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();

		// Act:
		final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session,
				this.getAccountId(ACCOUNTS[accountIndex]), topMostId, LIMIT, ReadOnlyTransferDao.TransferType.OUTGOING);
		final Collection<Integer> ids = pairs.stream().map(p -> (int) p.getTransfer().getId().longValue()).collect(Collectors.toList());

		// Assert:
		final Collection<Integer> expectedIds = this.getExpectedIdsForTransactions(this::getExpectedComparablePairsForOutgoingTransactions,
				accountIndex, topMostId, LIMIT);
		MatcherAssert.assertThat(ids, IsEquivalent.equivalentTo(expectedIds));
	}

	private List<Integer> getExpectedIdsForTransactions(final BiFunction<BlockHeight, Integer, List<Integer>> expectedIdsSupplier,
			final int accountIndex, final long topMostTransactionId, final int limit) {
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

	// endregion

	// region signature check

	@Test
	public void incomingOuterTransactionsHaveNonNullSignatures() {
		// Assert:
		this.assertOuterTransactionsHaveNonNullSignatures(ReadOnlyTransferDao.TransferType.INCOMING);
	}

	@Test
	public void outgoingOuterTransactionsHaveNonNullSignatures() {
		// Assert:
		this.assertOuterTransactionsHaveNonNullSignatures(ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	private void assertOuterTransactionsHaveNonNullSignatures(final ReadOnlyTransferDao.TransferType transferType) {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();
		for (final Account ACCOUNT : ACCOUNTS) {
			// Act:
			final Collection<TransferBlockPair> pairs = retriever.getTransfersForAccount(this.session, this.getAccountId(ACCOUNT),
					Long.MAX_VALUE, 100, transferType);

			// Assert:
			pairs.stream().forEach(p -> MatcherAssert.assertThat(p.getTransfer().getSenderProof(), IsNull.notNullValue()));
		}
	}

	// endregion

	// region all

	@Test
	public void cannotRetrieveAllTransactions() {
		// Arrange:
		final TransactionRetriever retriever = this.getTransactionRetriever();

		// Act:
		IntStream.range(0, ACCOUNTS.length).forEach(i -> ExceptionAssert.assertThrows(v -> {
			// Act:
			retriever.getTransfersForAccount(this.session, this.getAccountId(ACCOUNTS[i]), 34, LIMIT, ReadOnlyTransferDao.TransferType.ALL);
		}, IllegalArgumentException.class));
	}

	// endregion

	private void setupBlocks() {
		// Arrange: create 25 blocks (use height as the id)
		// first block starts with transaction id 1
		// second block starts with transaction id 1 + 4 + 2 + 1 + 1 + 1 + 1 + 6 * 4 = 1 + 34 = 35
		// third block starts with transaction id 1 + 2 * 34 = 69 and so on
		// unfortunately hibernate inserts the transaction in alphabetical order of the list names in DbBlock.
		// Thus the ids for the transactions in a block are (x being a non negative multiple of TRANSACTIONS_PER_BLOCK):
		// x + 1: regular importance transfer 1
		// x + 2: regular importance transfer 2
		// x + 3: multisig importance transfer
		// x + 4: regular mosaic definition creation transaction
		// x + 5: multisig mosaic definition creation transaction
		// x + 6: regular mosaic supply change transaction
		// x + 7: multisig mosaic supply change transaction
		// x + 8: regular modification transaction
		// x + 9: multisig modification transaction
		// x + 10: multisig transaction 1 (transfer)
		// x + 11: multisig transfer transaction
		// x + 12: multisig signature 1 for multisig transaction 1
		// x + 13: multisig signature 2 for multisig transaction 1
		// x + 14: multisig transaction 2 (importance transfer)
		// x + 15: multisig signature 1 for multisig transaction 2
		// x + 16: multisig signature 2 for multisig transaction 2
		// x + 17: multisig transaction 3 (modification)
		// x + 18: multisig signature 1 for multisig transaction 3
		// x + 19: multisig signature 2 for multisig transaction 3
		// x + 20: multisig transaction 4 (namespace provision)
		// x + 21: multisig provision namespace transaction
		// x + 22: multisig signature 1 for multisig transaction 4
		// x + 23: multisig signature 2 for multisig transaction 4
		// x + 24: multisig transaction 5 (mosaic definition creation)
		// x + 25: multisig signature 1 for multisig transaction 5
		// x + 26: multisig signature 2 for multisig transaction 5
		// x + 27: multisig transaction 6 (mosaic supply change)
		// x + 28: multisig signature 1 for multisig transaction 6
		// x + 29: multisig signature 2 for multisig transaction 6
		// x + 30: regular provision namespace transaction
		// x + 31: regular transfer 1
		// x + 32: regular transfer 2
		// x + 33: regular transfer 3
		// x + 34: regular transfer 4

		if (0 < this.blockDao.count()) {
			return;
		}

		// need to update the account state cache
		// account 0 is the sender of the outer transaction
		// account 1 is the multisig account (sender of the inner transaction)
		// account 2 is the recipient / remote / added cosignatory
		// account 3 is the sender of the signature transaction
		// We put account 2 into the list of cosignatories to test if an account that
		// hasn't initiated or signed a multisig transaction which it is cosignatory of
		// still can see the transaction as outgoing.
		final SynchronizedAccountStateCache cache = this.accountStateCache.copy();
		final AccountState state = cache.findStateByAddress(ACCOUNTS[1].getAddress());
		state.getMultisigLinks().addCosignatory(ACCOUNTS[0].getAddress());
		state.getMultisigLinks().addCosignatory(ACCOUNTS[2].getAddress());
		state.getMultisigLinks().addCosignatory(ACCOUNTS[3].getAddress());
		state.getMultisigLinks().addCosignatory(ACCOUNTS[4].getAddress());
		state.getMultisigLinks().incrementMinCosignatoriesBy(3);

		for (int i = 1; i <= 25; i++) {
			final Block block = NisUtils.createRandomBlockWithHeight(2 * i);

			this.addTransferTransactions(block);
			this.addImportanceTransferTransactions(block);
			this.addAggregateModificationTransaction(block);
			this.addProvisionNamespaceTransaction(block);
			this.addMosaicDefinitionCreationTransaction(block);
			this.addMosaicSupplyChangeTransaction(block);
			this.addMultisigTransactions(block);
			cache.commit();

			// Arrange: sign and map the blocks
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao), this.mosaicIdCache);
			this.blockDao.save(dbBlock);
		}
	}

	private void addTransferTransactions(final Block block) {
		// account 0 appears only as sender
		// account 1 appears only as receiver
		// account 2 appears as sender and receiver in different transactions
		// account 3 has one self transaction
		block.addTransaction(this.createTransfer((int) (block.getHeight().getRaw() * 100), ACCOUNTS[0], ACCOUNTS[1], true));
		block.addTransaction(this.createTransfer((int) (block.getHeight().getRaw() * 100 + 1), ACCOUNTS[0], ACCOUNTS[2], true));
		block.addTransaction(this.createTransfer((int) (block.getHeight().getRaw() * 100 + 2), ACCOUNTS[2], ACCOUNTS[1], true));
		block.addTransaction(this.createTransfer((int) (block.getHeight().getRaw() * 100 + 3), ACCOUNTS[3], ACCOUNTS[3], true));
	}

	private void addImportanceTransferTransactions(final Block block) {
		// account 0 + 2 appears only as sender
		// account 1 + 3 appears only as remote
		block.addTransaction(createImportanceTransfer((int) (block.getHeight().getRaw() * 100 + 4), ACCOUNTS[0], ACCOUNTS[1], true));
		block.addTransaction(createImportanceTransfer((int) (block.getHeight().getRaw() * 100 + 5), ACCOUNTS[2], ACCOUNTS[3], true));
	}

	private void addAggregateModificationTransaction(final Block block) {
		// account 0 is sender
		// accounts 1-3 are cosignatories
		block.addTransaction(createAggregateModificationTransaction((int) (block.getHeight().getRaw() * 100 + 6), ACCOUNTS[0],
				Arrays.asList(ACCOUNTS[1], ACCOUNTS[2], ACCOUNTS[3]), 1, true));
	}

	private void addProvisionNamespaceTransaction(final Block block) {
		// account 0 appears only as sender
		block.addTransaction(
				createProvisionNamespaceTransaction((int) (block.getHeight().getRaw() * 100 + 7), ACCOUNTS[0], ACCOUNTS[1], true));
	}

	private void addMosaicDefinitionCreationTransaction(final Block block) {
		// account 0 appears only as sender
		block.addTransaction(createMosaicDefinitionCreationTransaction((int) (block.getHeight().getRaw() * 100 + 8), ACCOUNTS[0], true));
	}

	private void addMosaicSupplyChangeTransaction(final Block block) {
		// account 0 appears only as sender
		block.addTransaction(createMosaicSupplyChangeTransaction((int) (block.getHeight().getRaw() * 100 + 9), ACCOUNTS[0], true));
	}

	private void addMultisigTransactions(final Block block) {
		// account 0 is outer transaction sender
		// account 1 is inner transaction sender
		// account 2 is recipient/remote/added cosignatory
		// account 3 is sender of signature transaction
		// account 4 is sender of signature transaction
		block.addTransaction(this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 10), TransactionTypes.TRANSFER));
		block.addTransaction(
				this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 11), TransactionTypes.IMPORTANCE_TRANSFER));
		block.addTransaction(this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 12),
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION));
		block.addTransaction(
				this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 13), TransactionTypes.PROVISION_NAMESPACE));
		block.addTransaction(
				this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 14), TransactionTypes.MOSAIC_DEFINITION_CREATION));
		block.addTransaction(
				this.createMultisigTransaction((int) (block.getHeight().getRaw() * 100 + 15), TransactionTypes.MOSAIC_SUPPLY_CHANGE));
	}

	private Transaction createTransfer(final int timeStamp, final Account sender, final Account recipient, final boolean signTransaction) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		IntStream.range(1, 11).forEach(i -> {
			final MosaicId mosaicId = Utils.createMosaicId(i * 10);
			if (!this.mosaicIdCache.contains(new DbMosaicId((long) (1000 + i)))) {
				this.mosaicIdCache.add(mosaicId, new DbMosaicId((long) (1000 + i)));
			}

			attachment.addMosaic(mosaicId, Quantity.fromValue(i * 10));
		});
		final Transaction transaction = new TransferTransaction(new TimeInstant(timeStamp), sender, recipient, Amount.fromNem(8),
				attachment);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createImportanceTransfer(final int timeStamp, final Account sender, final Account remote,
			final boolean signTransaction) {
		final Transaction transaction = new ImportanceTransferTransaction(new TimeInstant(timeStamp), sender,
				ImportanceTransferMode.Activate, remote);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createAggregateModificationTransaction(final int timeStamp, final Account sender,
			final Collection<Account> cosignatories, final int relativeMinCosignatoriesChange, final boolean signTransaction) {
		final List<MultisigCosignatoryModification> modifications = cosignatories.stream()
				.map(c -> createModification(MultisigModificationType.AddCosignatory, c)).collect(Collectors.toList());
		final Transaction transaction = new MultisigAggregateModificationTransaction(new TimeInstant(timeStamp), sender, modifications,
				new MultisigMinCosignatoriesModification(relativeMinCosignatoriesChange));
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createProvisionNamespaceTransaction(final int timeStamp, final Account sender, final Account rentalFeeSink,
			final boolean signTransaction) {
		final Transaction transaction = new ProvisionNamespaceTransaction(new TimeInstant(timeStamp), sender, rentalFeeSink,
				Amount.fromNem(25000), new NamespaceIdPart("bar"), new NamespaceId("foo"));
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createMosaicDefinitionCreationTransaction(final int timeStamp, final Account sender,
			final boolean signTransaction) {
		final Transaction transaction = RandomTransactionFactory.createMosaicDefinitionCreationTransaction(new TimeInstant(timeStamp),
				sender);
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private static Transaction createMosaicSupplyChangeTransaction(final int timeStamp, final Account sender,
			final boolean signTransaction) {
		final Transaction transaction = new MosaicSupplyChangeTransaction(new TimeInstant(timeStamp), sender, Utils.createMosaicId(10),
				MosaicSupplyType.Create, Supply.fromValue(123));
		if (signTransaction) {
			transaction.sign();
		}

		return transaction;
	}

	private Transaction createMultisigTransaction(final int timeStamp, final int innerType) {
		final Transaction innerTransaction;
		switch (innerType) {
			case TransactionTypes.TRANSFER:
				innerTransaction = this.createTransfer(timeStamp, ACCOUNTS[1], ACCOUNTS[2], false);
				break;
			case TransactionTypes.IMPORTANCE_TRANSFER:
				innerTransaction = createImportanceTransfer(timeStamp, ACCOUNTS[1], ACCOUNTS[2], false);
				break;
			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				innerTransaction = createAggregateModificationTransaction(timeStamp, ACCOUNTS[1], Collections.singletonList(ACCOUNTS[2]), 3,
						false);
				break;
			case TransactionTypes.PROVISION_NAMESPACE:
				innerTransaction = createProvisionNamespaceTransaction(timeStamp, ACCOUNTS[1], ACCOUNTS[2], false);
				break;
			case TransactionTypes.MOSAIC_DEFINITION_CREATION:
				innerTransaction = createMosaicDefinitionCreationTransaction(timeStamp, ACCOUNTS[1], false);
				break;
			case TransactionTypes.MOSAIC_SUPPLY_CHANGE:
				innerTransaction = createMosaicSupplyChangeTransaction(timeStamp, ACCOUNTS[1], false);
				break;
			default :
				throw new RuntimeException("invalid inner transaction type.");
		}

		final Hash hash = HashUtils.calculateHash(innerTransaction);

		final MultisigTransaction multisig = new MultisigTransaction(new TimeInstant(timeStamp), ACCOUNTS[0], innerTransaction);
		multisig.addSignature(createSignature(timeStamp, ACCOUNTS[3], ACCOUNTS[1], hash));
		multisig.addSignature(createSignature(timeStamp, ACCOUNTS[4], ACCOUNTS[1], hash));
		multisig.sign();
		return multisig;
	}

	private static MultisigSignatureTransaction createSignature(final int timeStamp, final Account cosigner, final Account multisig,
			final Hash hash) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(new TimeInstant(timeStamp), cosigner, multisig,
				hash);
		transaction.sign();
		return transaction;
	}

	private static MultisigCosignatoryModification createModification(final MultisigModificationType type, final Account account) {
		return new MultisigCosignatoryModification(type, account);
	}

	protected Long getAccountId(final Account account) {
		return DaoUtils.getAccountId(this.session, account.getAddress());
	}
}
