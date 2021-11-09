package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.MosaicIdCache;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
@SuppressWarnings("rawtypes")
public class BlockDaoTest {

	@ContextConfiguration(classes = TestConf.class)
	@RunWith(SpringJUnit4ClassRunner.class)
	private static abstract class Base {
		@Autowired
		protected AccountDao accountDao;

		@Autowired
		protected TransferDao transferDao;

		@Autowired
		protected BlockDao blockDao;

		@Autowired
		protected MosaicIdCache mosaicIdCache;

		@Autowired
		protected SessionFactory sessionFactory;

		protected Session session;

		@Before
		public void before() {
			Utils.setupGlobals();

			if (null != this.sessionFactory) {
				this.session = this.sessionFactory.openSession();
			}
		}

		@After
		public void after() {
			if (null != this.session) {
				DbTestUtils.dbCleanup(this.session);
				this.mosaicIdCache.clear();
				this.session.close();
			}

			Utils.resetGlobals();
		}

		// region helpers

		protected AccountDaoLookup prepareMapping(final Object... accounts) {
			// Arrange:
			final MockAccountDao mockAccountDao = new MockAccountDao();
			for (final Object o : accounts) {
				final Account a = (Account) o;
				final Address address = a.getAddress();
				final DbAccount dbA = new DbAccount(address);
				mockAccountDao.addMapping(a, dbA);
			}
			return new AccountDaoLookupAdapter(mockAccountDao);
		}

		protected org.nem.core.model.Block createTestEmptyBlock(final Account signer, final long height, final int i) {
			final Hash generationHash = HashUtils.nextHash(Hash.ZERO, signer.getAddress().getPublicKey());
			final org.nem.core.model.Block emptyBlock = new org.nem.core.model.Block(signer, Hash.ZERO, generationHash,
					new TimeInstant(123 + i), new BlockHeight(height));
			emptyBlock.sign();
			return emptyBlock;
		}

		// endregion
	}

	public static class General extends Base {

		// region save

		@Test
		public void savingBlockSavesAccounts() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 123, 0);
			final DbBlock entity = toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(entity);

			// Assert:
			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());
		}

		@Test
		public void savingBlockSavesTransactions() {
			// Arrange:
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			final Block block = this.createBlockWithTransactions(new TimeInstant(123), new BlockHeight(111));

			this.addMappings(mockAccountDao, block);

			final DbBlock dbBlock = toDbModel(block, accountDaoLookup);
			this.blockDao.save(dbBlock);

			// Act:
			final DbBlock entity = this.blockDao.findByHeight(block.getHeight());

			// Assert:
			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());

			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final List<? extends AbstractBlockTransfer> blockTransactions = entry.getFromBlock.apply(entity);
				MatcherAssert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
				MatcherAssert.assertThat(blockTransactions.get(0).getId(), IsNull.notNullValue());
			}
		}

		@Test
		public void saveMultiSavesMultipleBlocksInDatabase() {
			// Arrange:
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

			final int numBlocks = 3;
			final List<DbBlock> blocks = new ArrayList<>();
			for (int i = 2; i < 2 + numBlocks; i++) {
				final Block dummyBlock = this.createBlockWithTransactions(new TimeInstant(i * 123), new BlockHeight(i));

				this.addMappings(mockAccountDao, dummyBlock);

				final DbBlock dbBlock = toDbModel(dummyBlock, accountDaoLookup);
				blocks.add(dbBlock);
			}

			// Act:
			this.blockDao.save(blocks);
			final Collection<DbBlock> reloadedBlocks = this.blockDao.getBlocksAfter(BlockHeight.ONE, 10);

			// Assert:
			MatcherAssert.assertThat(reloadedBlocks.size(), IsEqual.equalTo(numBlocks));
			for (final DbBlock block : blocks) {
				MatcherAssert.assertThat(this.blockDao.findByHeight(new BlockHeight(block.getHeight())), IsNull.notNullValue());
			}
		}

		// endregion

		// region retrieve

		@Test
		public void findByHeightReturnsBlockWithCorrectHeightIfBlockWithThatHeightExistsInDatabase() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 234, 0);
			final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(dbBlock);
			final DbBlock entity = this.blockDao.findByHeight(emptyBlock.getHeight());

			// Assert:
			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getId()));
			MatcherAssert.assertThat(entity.getHeight(), IsEqual.equalTo(emptyBlock.getHeight().getRaw()));
			MatcherAssert.assertThat(entity.getBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(emptyBlock)));
			MatcherAssert.assertThat(entity.getGenerationHash(), IsEqual.equalTo(emptyBlock.getGenerationHash()));
			MatcherAssert.assertThat(entity.getBlockTransferTransactions().size(), IsEqual.equalTo(0));
			MatcherAssert.assertThat(entity.getHarvester().getPublicKey(), IsEqual.equalTo(signer.getAddress().getPublicKey()));
			MatcherAssert.assertThat(entity.getHarvesterProof(), IsEqual.equalTo(emptyBlock.getSignature().getBytes()));
		}

		@Test
		public void findByHeightReturnsNullIfBlockWithThatHeightDoesNotExistInDatabase() {
			// Act:
			final DbBlock entity = this.blockDao.findByHeight(new BlockHeight(123L));

			// Assert:
			MatcherAssert.assertThat(entity, IsNull.nullValue());
		}

		@Test
		public void getBlocksForAccountDoesNotRetrieveTransfers() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456, 0);
			final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(dbBlock);
			final Collection<DbBlock> entities = this.blockDao.getBlocksForAccount(signer, null, 25);

			// Assert:
			MatcherAssert.assertThat(entities.size(), IsEqual.equalTo(1));
			final DbBlock entity = entities.iterator().next();

			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getId()));

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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

				// Act:
				this.blockDao.save(dbBlock);
				ids.add(dbBlock.getId());
			}

			final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);

			// Assert:
			// - 25 is expected because getBlocksForAccount returns both blocks harvested directly (15)
			// and blocks harvested remotely (15) up to the limit (25)
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
		}

		@Test
		public void getBlocksForAccountRespectsId() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

			final List<Long> ids = new ArrayList<>();
			for (int i = 0; i < 30; i++) {
				final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, 0);
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

				// Act:
				this.blockDao.save(dbBlock);

				ids.add(dbBlock.getId());
			}
			final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);
			final Collection<DbBlock> entities2 = this.blockDao.getBlocksForAccount(signer, ids.get(0), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(0));
		}

		@Test
		public void getBlocksForAccountReturnsBlocksSortedByHeight() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

			final List<Long> ids = new ArrayList<>();
			for (int i = 0; i < 30; i++) {
				final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, (23 * i + 3) % 30);
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

				// Act:
				this.blockDao.save(dbBlock);

				ids.add(dbBlock.getId());
			}
			final Collection<DbBlock> entities1 = this.blockDao.getBlocksForAccount(signer, null, 25);
			final Collection<DbBlock> entities2 = this.blockDao.getBlocksForAccount(signer, ids.get(29), 25);
			final Collection<DbBlock> entities3 = this.blockDao.getBlocksForAccount(signer, ids.get(0), 25);

			// Assert:
			final BiConsumer<Collection<DbBlock>, Long> assertCollectionContainsBlocksStartingAtHeight = (entities, startHeight) -> {
				MatcherAssert.assertThat(entities.size(), IsEqual.equalTo(25));

				long lastHeight = startHeight;
				for (final DbBlock entity : entities) {
					MatcherAssert.assertThat(entity.getHeight(), IsEqual.equalTo(lastHeight--));
				}
			};

			assertCollectionContainsBlocksStartingAtHeight.accept(entities1, 456L + 29);
			assertCollectionContainsBlocksStartingAtHeight.accept(entities2, 456L + 28);
			MatcherAssert.assertThat(entities3.size(), IsEqual.equalTo(0));
		}

		// endregion

		// region delete/modify

		@Test
		public void deleteBlockDoesNotRemoveAccounts() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 567, 0);
			final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(dbBlock);
			this.blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
			final DbAccount entity = this.accountDao.getAccountByPrintableAddress(dbBlock.getHarvester().getPrintableKey());

			// Assert:
			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getId(), IsEqual.equalTo(dbBlock.getHarvester().getId()));
			MatcherAssert.assertThat(entity.getPublicKey(), IsEqual.equalTo(signer.getAddress().getPublicKey()));
		}

		@Test
		public void deleteBlockRemovesTransactions() {
			// Arrange:
			final Collection<String> transactionTables = TestTransactionRegistry.stream().map(e -> e.tableName).filter(tn -> null != tn)
					.collect(Collectors.toList());

			// Assert: preconditions
			for (final String table : transactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
			}

			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			final Block block = this.createBlockWithTransactions(new TimeInstant(123), new BlockHeight(111));

			this.addMappings(mockAccountDao, block);

			final DbBlock dbBlock = toDbModel(block, accountDaoLookup);
			this.blockDao.save(dbBlock);

			// Assert: preconditions
			for (final String table : transactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table) > 0, IsEqual.equalTo(true));
			}

			this.blockDao.deleteBlocksAfterHeight(block.getHeight().prev());

			// Assert:
			for (final String table : transactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
			}
		}

		@Test
		public void deleteBlockRemovesEntriesFromNonTransactionTables() {
			// Assert: preconditions
			final String[] nonTransactionTables = {
					"MultisigSends", "MultisigReceives", "MultisigModifications", "MinCosignatoriesModifications", "Namespaces",
					"MosaicDefinitions", "MosaicProperties", "TransferredMosaics"
			};

			for (final String table : nonTransactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
			}

			// Arrange:
			final Account issuer = Utils.generateRandomAccount();
			final Account multisig = Utils.generateRandomAccount();
			final Account cosignatory = Utils.generateRandomAccount();
			final Account cosignatoryToAdd = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(issuer, multisig, cosignatory, cosignatoryToAdd);
			final org.nem.core.model.Block block = this.createTestEmptyBlock(issuer, 678, 0);
			block.addTransaction(
					this.prepareMultisigMultisigAggregateModificationTransaction(issuer, multisig, cosignatory, cosignatoryToAdd));
			block.addTransaction(sign(RandomTransactionFactory.createProvisionNamespaceTransaction()));
			block.addTransaction(sign(RandomTransactionFactory.createMosaicDefinitionCreationTransaction()));
			block.addTransaction(sign(this.prepareTransferTransactionWithAttachment()));
			block.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(block, accountDaoLookup, this.mosaicIdCache);
			this.blockDao.save(dbBlock);
			for (final String table : nonTransactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table) > 0, IsEqual.equalTo(true));
			}

			// Act:
			this.blockDao.deleteBlocksAfterHeight(block.getHeight().prev());

			// Assert:
			for (final String table : nonTransactionTables) {
				MatcherAssert.assertThat(this.getScanCount(table), IsEqual.equalTo(0L));
			}
		}

		private long getScanCount(final String tableName) {
			final Session session = this.sessionFactory.openSession();
			final Long count = (Long) session.createSQLQuery("SELECT COUNT(*) as count FROM " + tableName)
					.addScalar("count", LongType.INSTANCE).uniqueResult();
			session.flush();
			session.clear();
			session.close();
			return count;
		}

		// endregion

		// region getters

		@Test
		public void getHashesFromReturnsProperHashes() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

			final ArrayList<Hash> expectedHashes = new ArrayList<>(30);
			for (int i = 0; i < 30; i++) {
				final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedHashes.add(dbBlock.getBlockHash());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
			final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final Hash entity : entities1.asCollection()) {
				MatcherAssert.assertThat(entity, IsEqual.equalTo(expectedHashes.get(i)));
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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedHashes.put((i * 23 + 3) % 30, dbBlock.getBlockHash());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
			final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final Hash entity : entities1.asCollection()) {
				MatcherAssert.assertThat(entity, IsEqual.equalTo(expectedHashes.get(i)));
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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedDifficulties.add(dbBlock.getDifficulty());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
			final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final BlockDifficulty entity : entities1) {
				MatcherAssert.assertThat(entity.getRaw(), IsEqual.equalTo(expectedDifficulties.get(i)));
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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedDifficulties.put((i * 23 + 3) % 30, dbBlock.getDifficulty());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
			final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final BlockDifficulty entity : entities1) {
				MatcherAssert.assertThat(entity.getRaw(), IsEqual.equalTo(expectedDifficulties.get(i)));
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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedTimestamps.add(dbBlock.getTimeStamp());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
			final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final TimeInstant entity : entities1) {
				MatcherAssert.assertThat(entity.getRawTime(), IsEqual.equalTo(expectedTimestamps.get(i)));
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
				final DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);
				expectedTimeStamps.put((i * 23 + 3) % 30, dbBlock.getTimeStamp());

				// Act:
				this.blockDao.save(dbBlock);
			}
			final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
			final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

			// Assert:
			MatcherAssert.assertThat(entities1.size(), IsEqual.equalTo(25));
			MatcherAssert.assertThat(entities2.size(), IsEqual.equalTo(10));

			int i = 0;
			for (final TimeInstant entity : entities1) {
				MatcherAssert.assertThat(entity.getRawTime(), IsEqual.equalTo(expectedTimeStamps.get(i)));
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
			MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(5));
		}

		@Test
		public void getBlocksAfterReturnsAllBlocksAfterGivenHeightIfNotEnoughBlocksAreAvailable() {
			// Arrange:
			this.createBlocksInDatabase(2, 11);

			// Act:
			final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 15);

			// Assert:
			MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(9));
		}

		@Test
		public void getBlocksAfterReturnsBlocksWithTransactions() {
			// Arrange:
			this.createBlocksInDatabaseWithTransactions(2, 4);

			// Act:
			final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(BlockHeight.ONE, 10);

			// Assert:
			MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(3));
			for (final DbBlock block : blocks) {
				final int numExpectedTransactions = TransactionRegistry.size() + 1; // 1 multisig inner
				MatcherAssert.assertThat(DbBlockExtensions.countTransactions(block), IsEqual.equalTo(numExpectedTransactions));
			}
		}

		@Test
		public void getBlocksAfterReturnsBlocksAfterGivenHeight() {
			// Arrange:
			this.createBlocksInDatabase(2, 11);

			// Act:
			final Collection<DbBlock> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 6);

			// Assert:
			MatcherAssert.assertThat(blocks.stream().findFirst().get().getHeight(), IsEqual.equalTo(3L));
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
					MatcherAssert.assertThat(previousDbBlock.getHeight(), IsEqual.equalTo(block.getHeight() - 1));
				}
				previousDbBlock = block;
			}
		}

		// endregion

		// mosaicIdCache

		@Test
		public void saveBlockUpdatesMosaicIdCache() {
			// Arrange:
			final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.vouchers"), "alice's gift vouchers");
			final DbBlock dbBlock = this.prepareBlock();

			// sanity check
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1));

			// Act:
			this.blockDao.save(dbBlock);

			// Assert
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1 + 1));
			MatcherAssert.assertThat(this.mosaicIdCache.get(mosaicId), IsEqual.equalTo(new DbMosaicId(1L)));
		}

		@Test
		public void getBlocksAfterAndUpdateCacheUpdatesMosaicIdCache() {
			// Assert:
			this.assertMosaicCacheUpdateBehavior(true, new DbMosaicId(1L));
		}

		@Test
		public void getBlocksAfterDoesNotUpdateMosaicIdCache() {
			// Assert:
			this.assertMosaicCacheUpdateBehavior(false, null);
		}

		private void assertMosaicCacheUpdateBehavior(final boolean updateCache, final DbMosaicId expectedDbMosaicId) {
			// Arrange:
			final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.vouchers"), "alice's gift vouchers");
			final DbBlock dbBlock = this.prepareBlock();

			// sanity check
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1));

			this.blockDao.save(dbBlock);

			// sanity check
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1 + 1));
			this.mosaicIdCache.remove(mosaicId);
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1));

			// Act:
			if (updateCache) {
				this.blockDao.getBlocksAfterAndUpdateCache(new BlockHeight(100), 100); // block height is 111
			} else {
				this.blockDao.getBlocksAfter(new BlockHeight(100), 100); // block height is 111
			}

			final DbMosaicId mosaicIdFromCache = this.mosaicIdCache.get(mosaicId);

			// Assert:
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1 + (updateCache ? 1 : 0)));
			MatcherAssert.assertThat(mosaicIdFromCache, IsEqual.equalTo(expectedDbMosaicId));
		}

		@Test
		public void deleteBlocksAfterHeightUpdatesMosaicIdCache() {
			// Arrange:
			final DbBlock dbBlock = this.prepareBlock();

			// sanity check
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1));

			this.blockDao.save(dbBlock);

			// sanity check
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1 + 1));

			// Act:
			this.blockDao.deleteBlocksAfterHeight(new BlockHeight(100));

			// Assert
			MatcherAssert.assertThat(this.mosaicIdCache.size(), IsEqual.equalTo(1));
		}

		// endregion

		// region helpers

		private static Transaction sign(final Transaction transaction) {
			transaction.sign();
			return transaction;
		}

		private TransferTransaction prepareTransferTransactionWithAttachment() {
			// use a mosaic with db id != 1 so that the addToMosaicIdsCache operation succeeds
			// (the block has one mosaic definition creation transaction) and does not create
			// conflicting db mosaic ids
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaic(Utils.createMosaicId(1), Quantity.fromValue(100));
			this.mosaicIdCache.add(Utils.createMosaicId(1), new DbMosaicId(1000L));
			return RandomTransactionFactory.createTransferWithAttachment(attachment);
		}

		private MultisigTransaction prepareMultisigMultisigAggregateModificationTransaction(final Account issuer, final Account multisig,
				final Account cosignatory, final Account cosignatoryToAdd) {
			final MultisigCosignatoryModification cosignatoryModification = new MultisigCosignatoryModification(
					MultisigModificationType.AddCosignatory, cosignatoryToAdd);
			final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(TimeInstant.ZERO,
					multisig, Collections.singletonList(cosignatoryModification), new MultisigMinCosignatoriesModification(1));
			return this.prepareMultisigTransaction(transaction, issuer, multisig, cosignatory);
		}

		private MultisigTransaction prepareMultisigTransaction(final Transaction transaction, final Account issuer, final Account multisig,
				final Account cosignatory) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(TimeInstant.ZERO, cosignatory,
					multisig, transaction);
			signatureTransaction.sign();
			final MultisigTransaction multisigTransaction = new MultisigTransaction(TimeInstant.ZERO, issuer, transaction);
			multisigTransaction.sign();
			multisigTransaction.addSignature(signatureTransaction);
			return multisigTransaction;
		}

		private void createBlocksInDatabase(final int startHeight, final int endHeight) {
			final Account sender = Utils.generateRandomAccount();
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			this.addMapping(mockAccountDao, sender);

			for (int i = startHeight; i <= endHeight; i++) {
				final org.nem.core.model.Block dummyBlock = new org.nem.core.model.Block(sender, Hash.ZERO, Hash.ZERO,
						new TimeInstant(i * 123), new BlockHeight(i));
				final Account recipient = Utils.generateRandomAccount();
				this.addMapping(mockAccountDao, recipient);
				dummyBlock.sign();
				final DbBlock dbBlock = toDbModel(dummyBlock, accountDaoLookup);
				this.blockDao.save(dbBlock);
			}
		}

		private DbBlock prepareBlock() {
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
			final Block block = this.createBlockWithTransactions(new TimeInstant(123), new BlockHeight(111));
			this.addMappings(mockAccountDao, block);
			return toDbModel(block, accountDaoLookup);
		}

		private Block createBlockWithTransactions(final TimeInstant timeStamp, final BlockHeight height) {
			// Arrange:
			final Map<Integer, Supplier<Transaction>> transactionFactories = new HashMap<>();
			for (final TestTransactionRegistry.Entry<Transaction> entry : TestTransactionRegistry.iterate()) {
				if (!TransactionTypes.getBlockEmbeddableTypes().contains(entry.type)) {
					continue;
				}

				transactionFactories.put(entry.type, () -> sign(entry.createModel.get()));
			}

			// Sanity:
			MatcherAssert.assertThat(transactionFactories.size(), IsEqual.equalTo(TransactionRegistry.size()));

			final Account blockSigner = Utils.generateRandomAccount();
			final Block dummyBlock = new Block(blockSigner, Hash.ZERO, Hash.ZERO, timeStamp, height);

			for (final Map.Entry<Integer, Supplier<Transaction>> entry : transactionFactories.entrySet()) {
				final Transaction transaction = entry.getValue().get();
				dummyBlock.addTransaction(transaction);

				// Sanity:
				MatcherAssert.assertThat(transaction.getType(), IsEqual.equalTo(entry.getKey()));
			}

			dummyBlock.sign();
			return dummyBlock;
		}

		private void createBlocksInDatabaseWithTransactions(final int startHeight, final int endHeight) {
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

			for (int i = startHeight; i <= endHeight; i++) {
				final Block dummyBlock = this.createBlockWithTransactions(new TimeInstant(i * 123), new BlockHeight(i));

				this.addMappings(mockAccountDao, dummyBlock);

				final DbBlock dbBlock = toDbModel(dummyBlock, accountDaoLookup);
				this.blockDao.save(dbBlock);
			}
		}

		private void addMappings(final MockAccountDao mockAccountDao, final Block block) {
			mockAccountDao.addMappings(block);
		}

		private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
			mockAccountDao.addMapping(account);
		}

		// endregion
	}

	@RunWith(Parameterized.class)
	public static class PerTransaction extends Base {
		private TestContextManager testContextManager;
		private final TransactionRegistry.Entry<? extends AbstractTransfer, ? extends Transaction> entry;
		private final Supplier<? extends Transaction> createModel;

		public PerTransaction(final int type) {
			this.entry = TransactionRegistry.findByType(type);
			this.createModel = TestTransactionRegistry.findByType(type).createModel;
		}

		@Before
		public void setUpContext() throws Exception {
			// manually initialize spring like the runner would do for us (but we can't use because we want parameterized tests)
			this.testContextManager = new TestContextManager(this.getClass());
			this.testContextManager.prepareTestInstance(this);

			// needed because Base::before is run *first* prior to any dependency injections
			this.session = this.sessionFactory.openSession();
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getMultisigEmbeddableTypes());
		}

		// region assertSavingBlockSavesTransaction

		@Test
		public void savingBlockSavesTransactions() {
			// Assert:
			this.assertSavingBlockSavesTransaction(this.entry.type, this::prepareTransaction);
		}

		private void assertSavingBlockSavesTransaction(final int transactionType, final Supplier<Transaction> createTransaction) {
			// Arrange:
			final Transaction transaction = createTransaction.get();
			final AccountDaoLookup accountDaoLookup = this.prepareMapping(transaction.getAccounts().toArray());

			final Account signer = Utils.generateRandomAccount();
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 133, 0);
			emptyBlock.addTransaction(transaction);
			emptyBlock.sign();

			final DbBlock entity = toDbModel(emptyBlock, accountDaoLookup);

			// Act:
			this.blockDao.save(entity);

			// Assert:
			MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
			MatcherAssert.assertThat(entity.getHarvester().getId(), IsNull.notNullValue());

			int numTransactions = 0;
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final List<? extends AbstractBlockTransfer> blockTransactions = entry.getFromBlock.apply(entity);
				numTransactions += blockTransactions.size();

				if (transactionType == entry.type) {
					MatcherAssert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
					MatcherAssert.assertThat(blockTransactions.get(0).getId(), IsNull.notNullValue());
				} else {
					MatcherAssert.assertThat(blockTransactions.size(), IsEqual.equalTo(0));
				}
			}

			MatcherAssert.assertThat(numTransactions, IsEqual.equalTo(1));
		}

		// endregion

		// region assertSavingBlockDoesNotChangeTransferBlockIndex

		@Test
		public void savingDoesNotChangeTransferBlockIndex() {
			// Assert:
			this.assertSavingBlockDoesNotChangeTransferBlockIndex(this.entry.getFromBlock::apply, this::prepareTransaction, false);
		}

		@Test
		public void reloadAfterSavingDoesNotChangeTransferBlockIndex() {
			// Assert:
			this.assertSavingBlockDoesNotChangeTransferBlockIndex(this.entry.getFromBlock::apply, this::prepareTransaction, true);
		}

		private void assertSavingBlockDoesNotChangeTransferBlockIndex(
				final Function<DbBlock, List<? extends AbstractBlockTransfer>> getTransfers, final Supplier<Transaction> createTransaction,
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
			DbBlock dbBlock = toDbModel(emptyBlock, accountDaoLookup);

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
			MatcherAssert.assertThat(transfers.size(), IsEqual.equalTo(2));
			MatcherAssert.assertThat(transfers.get(0).getBlkIndex(), IsEqual.equalTo(24));
			MatcherAssert.assertThat(transfers.get(1).getBlkIndex(), IsEqual.equalTo(12));

			final Hash h1 = transfers.get(0).getTransferHash();
			final Hash h2 = transfers.get(1).getTransferHash();
			MatcherAssert.assertThat(h1, IsEqual.equalTo(HashUtils.calculateHash(transfer1)));
			MatcherAssert.assertThat(h2, IsEqual.equalTo(HashUtils.calculateHash(transfer2)));
		}

		// endregion

		private Transaction prepareTransaction() {
			final Transaction transaction = this.createModel.get();
			transaction.sign();
			return transaction;
		}
	}

	private static DbBlock toDbModel(final Block block, final AccountDaoLookup accountDaoLookup) {
		return MapperUtils.toDbModelWithHack(block, accountDaoLookup);
	}
}
