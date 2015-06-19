package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class BlockDaoTest {
	@Autowired
	AccountDao accountDao;

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbUtils.dbCleanup(this.session);
		this.session.close();
	}

	//region save

	@Test
	public void savingBlockSavesAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 123, 0);
		final DbBlock entity = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());
	}

	//region assertSavingBlockSavesTransaction

	@Test
	public void savingBlockSavesTransferTransactions() {
		// Assert:
		this.assertSavingBlockSavesTransaction(
				TransactionTypes.TRANSFER,
				this::prepareTransferTransaction);
	}

	@Test
	public void savingBlockSavesImportanceTransferTransactions() {
		// Assert:
		this.assertSavingBlockSavesTransaction(
				TransactionTypes.IMPORTANCE_TRANSFER,
				this::prepareImportanceTransferTransaction);
	}

	@Test
	public void savingBlockSavesMultisigTransferTransaction() {
		// Assert:
		this.assertSavingBlockSavesTransaction(
				TransactionTypes.MULTISIG,
				this::prepareMultisigTransferTransaction);
	}

	@Test
	public void savingBlockSavesAggregateMultisigModificationTransferTransaction() {
		// Assert:
		this.assertSavingBlockSavesTransaction(
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				this::prepareMultisigModificationTransaction);
	}

	@Test
	public void savingBlockSavesProvisionNamespaceTransactions() {
		// Assert:
		this.assertSavingBlockSavesTransaction(
				TransactionTypes.PROVISION_NAMESPACE,
				this::prepareProvisionNamespaceTransaction);
	}

	private void assertSavingBlockSavesTransaction(
			final int transactionType,
			final Supplier<Transaction> createTransaction) {
		// Arrange:
		final Transaction transaction = createTransaction.get();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(transaction.getAccounts().toArray());

		final Account signer = Utils.generateRandomAccount();
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 133, 0);
		emptyBlock.addTransaction(transaction);
		emptyBlock.sign();

		final DbBlock entity = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());

		int numTransactions = 0;
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			final List<? extends AbstractBlockTransfer> blockTransactions = entry.getFromBlock.apply(entity);
			numTransactions += blockTransactions.size();

			if (transactionType == entry.type) {
				Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
				Assert.assertThat(blockTransactions.get(0).getId(), IsNull.notNullValue());
			} else {
				Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(0));
			}
		}

		Assert.assertThat(numTransactions, IsEqual.equalTo(1));
	}

	//endregion

	@Test
	public void savingBlockSavesTransactions() {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		final Block block = this.createBlockWithTransactions(new TimeInstant(123), new BlockHeight(111));

		this.addMappings(mockAccountDao, block);

		final DbBlock dbBlock = MapperUtils.toDbModel(block, accountDaoLookup);
		this.blockDao.save(dbBlock);

		// Act:
		final DbBlock entity = this.blockDao.findByHeight(block.getHeight());

		// Assert:
		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());

		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			final List<? extends AbstractBlockTransfer> blockTransactions = entry.getFromBlock.apply(entity);
			Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
			Assert.assertThat(blockTransactions.get(0).getId(), IsNull.notNullValue());
		}
	}

	//region assertSavingBlockDoesNotChangeTransferBlockIndex

	@Test
	public void savingDoesNotChangeTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockTransferTransactions,
				this::prepareTransferTransaction,
				false);
	}

	@Test
	public void savingDoesNotChangeImportanceTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockImportanceTransferTransactions,
				this::prepareImportanceTransferTransaction,
				false);
	}

	@Test
	public void savingDoesNotChangeMultisigTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockMultisigTransactions,
				this::prepareMultisigTransferTransaction,
				false);
	}

	@Test
	public void savingDoesNotChangeAggregateMultisigModificationTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockMultisigAggregateModificationTransactions,
				this::prepareMultisigModificationTransaction,
				false);
	}

	@Test
	public void reloadAfterSavingDoesNotChangeTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockTransferTransactions,
				this::prepareTransferTransaction,
				true);
	}

	@Test
	public void reloadAfterSavingDoesNotChangeImportanceTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockImportanceTransferTransactions,
				this::prepareImportanceTransferTransaction,
				true);
	}

	@Test
	public void reloadAfterSavingDoesNotChangeMultisigTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockMultisigTransactions,
				this::prepareMultisigTransferTransaction,
				true);
	}

	@Test
	public void reloadAfterSavingDoesNotChangeAggregateMultisigModificationTransferTransactionBlockIndex() {
		// Assert:
		this.assertSavingBlockDoesNotChangeTransferBlockIndex(
				DbBlock::getBlockMultisigAggregateModificationTransactions,
				this::prepareMultisigModificationTransaction,
				true);
	}

	private void assertSavingBlockDoesNotChangeTransferBlockIndex(
			final Function<DbBlock, List<? extends AbstractBlockTransfer>> getTransfers,
			final Supplier<Transaction> createTransaction,
			final boolean reload) {
		// Arrange:
		final Transaction transfer1 = createTransaction.get();
		final Transaction transfer2 = createTransaction.get();

		final List<Account> allAccounts = new ArrayList<>();
		allAccounts.addAll(transfer1.getAccounts());
		allAccounts.addAll(transfer2.getAccounts());
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(allAccounts.toArray());

		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(Utils.generateRandomAccount(), 133, 0);
		emptyBlock.addTransaction(transfer1);
		emptyBlock.addTransaction(transfer2);
		emptyBlock.sign();
		DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		List<? extends AbstractBlockTransfer> transfers = getTransfers.apply(dbBlock);
		transfers.get(0).setBlkIndex(24);
		transfers.get(1).setBlkIndex(12);

		this.blockDao.save(dbBlock);

		if (reload) {
			dbBlock = this.blockDao.findByHeight(emptyBlock.getHeight());
		}

		// Assert:
		transfers = getTransfers.apply(dbBlock);
		Assert.assertThat(transfers.size(), IsEqual.equalTo(2));
		Assert.assertThat(transfers.get(0).getBlkIndex(), IsEqual.equalTo(24));
		Assert.assertThat(transfers.get(1).getBlkIndex(), IsEqual.equalTo(12));

		final Hash h1 = transfers.get(0).getTransferHash();
		final Hash h2 = transfers.get(1).getTransferHash();
		Assert.assertThat(h1, IsEqual.equalTo(HashUtils.calculateHash(transfer1)));
		Assert.assertThat(h2, IsEqual.equalTo(HashUtils.calculateHash(transfer2)));
	}

	//endregion

	@Test
	public void saveMultiSavesMultipleBlocksInDatabase() {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		final int numBlocks = 3;
		final List<DbBlock> blocks = new ArrayList<>();
		for (int i = 2; i < 2 + numBlocks; i++) {
			final Block dummyBlock = this.createBlockWithTransactions(
					new TimeInstant(i * 123),
					new BlockHeight(i));

			this.addMappings(mockAccountDao, dummyBlock);

			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);
			blocks.add(dbBlock);
		}

		// Act:
		this.blockDao.save(blocks);
		final Collection<DbBlock> reloadedBlocks = this.blockDao.getBlocksAfter(BlockHeight.ONE, 10);

		// Assert:
		Assert.assertThat(reloadedBlocks.size(), IsEqual.equalTo(numBlocks));
		for (final DbBlock block : blocks) {
			Assert.assertThat(this.blockDao.findByHeight(new BlockHeight(block.getHeight())), IsNull.notNullValue());
		}
	}

	//endregion

	// region retrieve

	@Test
	public void findByHeightReturnsBlockWithCorrectHeightIfBlockWithThatHeightExistsInDatabase() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 234, 0);
		final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		final DbBlock entity = this.blockDao.findByHeight(emptyBlock.getHeight());

		// Assert:
		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getId()));
		Assert.assertThat(entity.getHeight(), IsEqual.equalTo(emptyBlock.getHeight().getRaw()));
		Assert.assertThat(entity.getBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(emptyBlock)));
		Assert.assertThat(entity.getGenerationHash(), IsEqual.equalTo(emptyBlock.getGenerationHash()));
		Assert.assertThat(entity.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
		Assert.assertThat(entity.getHarvester().getPublicKey(), IsEqual.equalTo(signer.getAddress().getPublicKey()));
		Assert.assertThat(entity.getHarvesterProof(), IsEqual.equalTo(emptyBlock.getSignature().getBytes()));
	}

	@Test
	public void findByHeightReturnsNullIfBlockWithThatHeightDoesNotExistInDatabase() {
		// Act:
		final DbBlock entity = this.blockDao.findByHeight(new BlockHeight(123L));

		// Assert:
		Assert.assertThat(entity, IsNull.nullValue());
	}

	@Test
	public void getBlocksForAccountDoesNotRetrieveTransfers() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456, 0);
		final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		final Collection<DbBlock> entities = this.blockDao.getBlocksForAccount(signer, null, 25);

		// Assert:
		Assert.assertThat(entities.size(), IsEqual.equalTo(1));
		final DbBlock entity = entities.iterator().next();

		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getId()));

		ExceptionAssert.assertThrows(v -> entity.getBlockTransferTransactions().size(), LazyInitializationException.class);
	}

	@Test
	public void getBlocksForAccountReturnsBlockForagedViaRemoteAccount() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remoteAccount = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, remoteAccount, Utils.generateRandomAccount());

		final List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final Account blockSigner = (i % 2 == 0) ? signer : remoteAccount;
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(blockSigner, 456 + i, 0);
			if (i % 2 == 1) {
				emptyBlock.setLessor(signer);
			}
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			ids.add(dbBlock.getId());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);

		// Assert:
		// - 25 is expected because getBlocksForAccount returns both blocks harvested directly (15)
		//   and blocks harvested remotely (15) up to the limit (25)
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
	}

	@Test
	public void getBlocksForAccountRespectsId() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, 0);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(dbBlock);

			ids.add(dbBlock.getId());
		}
		final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);
		final Collection<DbBlock> entities2 = this.blockDao.getBlocksForAccount(signer, ids.get(0), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getBlocksForAccountReturnsBlocksSortedByHeight() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, (23 * i + 3) % 30);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(dbBlock);

			ids.add(dbBlock.getId());
		}
		final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, null, 25);
		final Collection<DbBlock> entities2 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);
		final Collection<DbBlock> entities3 = this.blockDao.getBlocksForAccount(signer, ids.get(0), 25);

		// Assert:
		final BiConsumer<Collection<DbBlock>, Long> assertCollectionContainsBlocksStartingAtHeight = (entities, startHeight) -> {
			Assert.assertThat(entities.size(), IsEqual.equalTo(25));

			long lastHeight = startHeight;
			for (final DbBlock entity : entities) {
				Assert.assertThat(entity.getHeight(), IsEqual.equalTo(lastHeight--));
			}
		};

		assertCollectionContainsBlocksStartingAtHeight.accept(entities1, 456L + 29);
		assertCollectionContainsBlocksStartingAtHeight.accept(entities2, 456L + 28);
		Assert.assertThat(entities3.size(), IsEqual.equalTo(0));
	}
	//endregion

	//region delete/modify
	@Test
	public void deleteBlockDoesNotRemoveAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 567, 0);
		final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		this.blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final DbAccount entity = this.accountDao.getAccount(dbBlock.getHarvester().getId());

		// Assert:
		Assert.assertThat(entity.getId(), IsNull.notNullValue());
		Assert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getHarvester().getId()));
		Assert.assertThat(entity.getPublicKey(), IsEqual.equalTo(signer.getAddress().getPublicKey()));
	}

	@Test
	public void deleteBlockRemovesTransactions() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, recipient);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 678, 0);
		emptyBlock.addTransaction(this.prepareTransferTransaction(signer, recipient, 10));
		emptyBlock.addTransaction(this.prepareTransferTransaction(signer, recipient, 20));
		emptyBlock.addTransaction(this.prepareTransferTransaction(signer, recipient, 30));
		emptyBlock.sign();
		final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
		final Supplier<Integer> getNumTransfers =
				() -> this.transferDao.getTransactionsForAccountUsingId(emptyBlock.getSigner(), null, ReadOnlyTransferDao.TransferType.ALL, 100).size();

		this.blockDao.save(dbBlock);
		final int numTransfersBeforeDelete = getNumTransfers.get();

		// Act:
		this.blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final int numTransfersAfterDelete = getNumTransfers.get();

		// Assert:
		Assert.assertThat(numTransfersBeforeDelete, IsEqual.equalTo(3));
		Assert.assertThat(numTransfersAfterDelete, IsEqual.equalTo(0));
	}

	@Test
	public void deleteBlockRemovesEntriesFromNonTransactionTables() {
		// Assert: preconditions
		final String[] nonTransactionTables = {
				"MultisigSends",
				"MultisigReceives",
				"MultisigModifications",
				"MinCosignatoriesModifications",
				"Namespaces"};
		for (final String table : nonTransactionTables) {
			Assert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
		}

		// Arrange:
		final Account issuer = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account cosignatoryToAdd = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(issuer, multisig, cosignatory, cosignatoryToAdd);
		final org.nem.core.model.Block block = this.createTestEmptyBlock(issuer, 678, 0);
		block.addTransaction(this.prepareMultisigMultisigAggregateModificationTransaction(issuer, multisig, cosignatory, cosignatoryToAdd));
		block.addTransaction(this.prepareProvisionNamespaceTransaction());
		block.sign();
		final DbBlock dbBlock = MapperUtils.toDbModel(block, accountDaoLookup);
		this.blockDao.save(dbBlock);
		for (final String table : nonTransactionTables) {
			Assert.assertThat(this.getScanCount(table) > 0, IsEqual.equalTo(true));
		}

		// Act:
		this.blockDao.deleteBlocksAfterHeight(block.getHeight().prev());

		// Assert:
		for (final String table : nonTransactionTables) {
			Assert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
		}
	}

	private long getScanCount(final String tableName) {
		final Session session = this.sessionFactory.openSession();
		final Long count = (Long)session.createSQLQuery("SELECT COUNT(*) as count FROM " + tableName)
				.addScalar("count", LongType.INSTANCE)
				.uniqueResult();
		session.flush();
		session.clear();
		session.close();
		return count;
	}

	//endregion

	//region getters
	@Test
	public void getHashesFromReturnsProperHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final ArrayList<Hash> expectedHashes = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedHashes.add(dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
		final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final Hash entity : entities1.asCollection()) {
			Assert.assertThat(entity, IsEqual.equalTo(expectedHashes.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getHashesFromReturnsHashesInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final TreeMap<Integer, Hash> expectedHashes = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			// mind that time is linear, so blocks are totally mixed when it comes to timestamp...
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedHashes.put((i * 23 + 3) % 30, dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
		final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final Hash entity : entities1.asCollection()) {
			Assert.assertThat(entity, IsEqual.equalTo(expectedHashes.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getDifficultiesFromReturnsProperDifficulties() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final ArrayList<Long> expectedDifficulties = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			emptyBlock.setDifficulty(new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + (i * 7000)));
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedDifficulties.add(dbBlock.getDifficulty());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
		final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final BlockDifficulty entity : entities1) {
			Assert.assertThat(entity.getRaw(), IsEqual.equalTo(expectedDifficulties.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getDifficultiesFromReturnsDifficultiesInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final TreeMap<Integer, Long> expectedDifficulties = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			emptyBlock.setDifficulty(new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + (i * 7000)));
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedDifficulties.put((i * 23 + 3) % 30, dbBlock.getDifficulty());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
		final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final BlockDifficulty entity : entities1) {
			Assert.assertThat(entity.getRaw(), IsEqual.equalTo(expectedDifficulties.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getTimeStampsFromReturnsProperTimeStamps() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final ArrayList<Integer> expectedTimestamps = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedTimestamps.add(dbBlock.getTimeStamp());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
		final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final TimeInstant entity : entities1) {
			Assert.assertThat(entity.getRawTime(), IsEqual.equalTo(expectedTimestamps.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getTimeStampsFromReturnsTimeStampsInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final TreeMap<Integer, Integer> expectedTimeStamps = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			// mind that time is linear, so blocks are totally mixed when it comes to timestamp...
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			final DbBlock dbBlock = MapperUtils.toDbModel(emptyBlock, accountDaoLookup);
			expectedTimeStamps.put((i * 23 + 3) % 30, dbBlock.getTimeStamp());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
		final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), IsEqual.equalTo(25));
		Assert.assertThat(entities2.size(), IsEqual.equalTo(10));

		int i = 0;
		for (final TimeInstant entity : entities1) {
			Assert.assertThat(entity.getRawTime(), IsEqual.equalTo(expectedTimeStamps.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getBlocksAfterReturnsCorrectNumberOfBlocksIfEnoughBlocksAreAvailable() {
		// Arrange:
		this.createBlocksInDatabase(2, 11);

		// Act:
		final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(3), 5);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(5));
	}

	@Test
	public void getBlocksAfterReturnsAllBlocksAfterGivenHeightIfNotEnoughBlocksAreAvailable() {
		// Arrange:
		this.createBlocksInDatabase(2, 11);

		// Act:
		final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 15);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(9));
	}

	@Test
	public void getBlocksAfterReturnsBlocksWithTransactions() {
		// Arrange:
		this.createBlocksInDatabaseWithTransactions(2, 4);

		// Act:
		final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(BlockHeight.ONE, 10);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(3));
		for (final DbBlock block : blocks) {
			final int numExpectedTransactions = TransactionRegistry.size() + 2; // 1 signature, 1 multisig inner
			Assert.assertThat(DbBlockExtensions.countTransactions(block), IsEqual.equalTo(numExpectedTransactions));
		}
	}

	@Test
	public void getBlocksAfterReturnsBlocksAfterGivenHeight() {
		// Arrange:
		this.createBlocksInDatabase(2, 11);

		// Act:
		final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 6);

		// Assert:
		Assert.assertThat(blocks.stream().findFirst().get().getHeight(), IsEqual.equalTo(3L));
	}

	@Test
	public void getBlocksAfterReturnsBlocksInAscendingOrderOfHeights() {
		// Arrange:
		this.createBlocksInDatabase(2, 11);

		// Act:
		final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 6);

		// Assert:
		DbBlock previousDbBlock = null;
		for (final DbBlock block : blocks) {
			if (null != previousDbBlock) {
				Assert.assertThat(previousDbBlock.getHeight(), IsEqual.equalTo(block.getHeight() - 1));
			}
			previousDbBlock = block;
		}
	}

	//endregion

	//region helpers
	private AccountDaoLookup prepareMapping(final Object... accounts) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		for (final Object o : accounts) {
			final Account a = (Account)o;
			final Address address = a.getAddress();
			final DbAccount dbA = new DbAccount(address);
			mockAccountDao.addMapping(a, dbA);
		}
		return new AccountDaoLookupAdapter(mockAccountDao);
	}

	private org.nem.core.model.Block createTestEmptyBlock(final Account signer, final long height, final int i) {
		final Hash generationHash = HashUtils.nextHash(Hash.ZERO, signer.getAddress().getPublicKey());
		final org.nem.core.model.Block emptyBlock = new org.nem.core.model.Block(signer,
				Hash.ZERO,
				generationHash,
				new TimeInstant(123 + i),
				new BlockHeight(height));
		emptyBlock.sign();
		return emptyBlock;
	}

	private TransferTransaction prepareTransferTransaction() {
		return this.prepareTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount(), 10);
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				TimeInstant.ZERO,
				sender,
				recipient,
				Amount.fromNem(amount),
				null);
		transferTransaction.sign();
		return transferTransaction;
	}

	private ImportanceTransferTransaction prepareImportanceTransferTransaction() {
		return this.prepareImportanceTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount(), true);
	}

	private ImportanceTransferTransaction prepareImportanceTransferTransaction(final Account sender, final Account remote, final boolean isTransfer) {
		// Arrange:
		final ImportanceTransferTransaction importanceTransferTransaction = new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				sender,
				isTransfer ? ImportanceTransferMode.Activate : ImportanceTransferMode.Deactivate,
				remote);
		importanceTransferTransaction.sign();
		return importanceTransferTransaction;
	}

	private MultisigAggregateModificationTransaction prepareMultisigModificationTransaction() {
		return this.prepareMultisigModificationTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount());
	}

	private MultisigAggregateModificationTransaction prepareMultisigModificationTransaction(final Account sender, final Account cosignatory) {
		// Arrange:
		final List<MultisigCosignatoryModification> modifications = Collections.singletonList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosignatory));
		final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				sender,
				modifications);
		transaction.sign();
		return transaction;
	}

	private ProvisionNamespaceTransaction prepareProvisionNamespaceTransaction() {
		return this.prepareProvisionNamespaceTransaction(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(25000),
				new NamespaceIdPart("bar"),
				new NamespaceId("foo"));
	}

	private ProvisionNamespaceTransaction prepareProvisionNamespaceTransaction(
			final Account sender,
			final Account lessor,
			final Amount rentalFee,
			final NamespaceIdPart part,
			final NamespaceId namespaceId) {
		// Arrange:
		final ProvisionNamespaceTransaction provisionNamespaceTransaction = new ProvisionNamespaceTransaction(
				TimeInstant.ZERO,
				sender,
				lessor,
				rentalFee,
				part,
				namespaceId);
		provisionNamespaceTransaction.sign();
		return provisionNamespaceTransaction;
	}

	private MultisigTransaction prepareMultisigTransferTransaction() {
		return this.prepareMultisigTransferTransaction(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
	}

	private MultisigTransaction prepareMultisigTransferTransaction(final Account issuer, final Account multisig, final Account cosignatory) {
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				multisig,
				Utils.generateRandomAccount(),
				Amount.fromNem(123),
				null);
		return this.prepareMultisigTransaction(transaction, issuer, multisig, cosignatory);
	}

	private MultisigTransaction prepareMultisigMultisigAggregateModificationTransaction(
			final Account issuer,
			final Account multisig,
			final Account cosignatory,
			final Account cosignatoryToAdd) {
		final MultisigCosignatoryModification cosignatoryModification = new MultisigCosignatoryModification(
				MultisigModificationType.AddCosignatory,
				cosignatoryToAdd);
		final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Collections.singletonList(cosignatoryModification),
				new MultisigMinCosignatoriesModification(1));
		return this.prepareMultisigTransaction(transaction, issuer, multisig, cosignatory);
	}

	private MultisigTransaction prepareMultisigTransaction(
			final Transaction transaction,
			final Account issuer,
			final Account multisig,
			final Account cosignatory) {
		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				cosignatory,
				multisig,
				transaction);
		signatureTransaction.sign();
		final MultisigTransaction multisigTransaction = new MultisigTransaction(TimeInstant.ZERO, issuer, transaction);
		multisigTransaction.sign();
		multisigTransaction.addSignature(signatureTransaction);
		return multisigTransaction;
	}

	private List<Hash> createBlocksInDatabase(final int startHeight, final int endHeight) {
		final List<Hash> hashes = new ArrayList<>();
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);

		for (int i = startHeight; i <= endHeight; i++) {
			final org.nem.core.model.Block dummyBlock = new org.nem.core.model.Block(
					sender,
					Hash.ZERO,
					Hash.ZERO,
					new TimeInstant(i * 123),
					new BlockHeight(i));
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);
			this.blockDao.save(dbBlock);
		}

		return hashes;
	}

	private Block createBlockWithTransactions(final TimeInstant timeStamp, final BlockHeight height) {
		// Arrange:
		final Map<Integer, Supplier<Transaction>> transactionFactories = new HashMap<Integer, Supplier<Transaction>>() {
			{
				this.put(TransactionTypes.TRANSFER, BlockDaoTest.this::prepareTransferTransaction);
				this.put(TransactionTypes.IMPORTANCE_TRANSFER, BlockDaoTest.this::prepareImportanceTransferTransaction);
				this.put(TransactionTypes.MULTISIG, BlockDaoTest.this::prepareMultisigTransferTransaction);
				this.put(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, BlockDaoTest.this::prepareMultisigModificationTransaction);
				this.put(TransactionTypes.PROVISION_NAMESPACE, BlockDaoTest.this::prepareProvisionNamespaceTransaction);
			}
		};

		// Sanity:
		Assert.assertThat(transactionFactories.size(), IsEqual.equalTo(TransactionRegistry.size()));

		final Account blockSigner = Utils.generateRandomAccount();
		final Block dummyBlock = new Block(blockSigner, Hash.ZERO, Hash.ZERO, timeStamp, height);

		for (final Map.Entry<Integer, Supplier<Transaction>> entry : transactionFactories.entrySet()) {
			final Transaction transaction = entry.getValue().get();
			dummyBlock.addTransaction(transaction);

			// Sanity:
			Assert.assertThat(transaction.getType(), IsEqual.equalTo(entry.getKey()));
		}

		dummyBlock.sign();
		return dummyBlock;
	}

	private void createBlocksInDatabaseWithTransactions(final int startHeight, final int endHeight) {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		for (int i = startHeight; i <= endHeight; i++) {
			final Block dummyBlock = this.createBlockWithTransactions(
					new TimeInstant(i * 123),
					new BlockHeight(i));

			this.addMappings(mockAccountDao, dummyBlock);

			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);
			this.blockDao.save(dbBlock);
		}
	}

	private void addMappings(final MockAccountDao mockAccountDao, final Block block) {
		this.addMapping(mockAccountDao, block.getSigner());
		for (final Account account : block.getTransactions().stream().flatMap(t -> t.getAccounts().stream()).collect(Collectors.toList())) {
			this.addMapping(mockAccountDao, account);
		}
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final DbAccount dbSender = new DbAccount(account.getAddress());
		mockAccountDao.addMapping(account, dbSender);
	}

	//endregion
}
