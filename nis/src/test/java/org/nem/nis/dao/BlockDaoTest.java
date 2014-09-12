package org.nem.nis.dao;

import org.hibernate.LazyInitializationException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.Consumer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class BlockDaoTest {
	@Autowired
	AccountDao accountDao;

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	//region save
	@Test
	public void savingBlockSavesAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 123, 0);
		final Block entity = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
	}

	@Test
	public void savingBlockSavesTransactions() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, recipient);
		final TransferTransaction transferTransaction = this.prepareTransferTransaction(recipient, signer, 10);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 133, 0);
		emptyBlock.addTransaction(transferTransaction);
		emptyBlock.sign();
		final Block entity = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
		Assert.assertThat(entity.getBlockTransfers().size(), not(equalTo(0)));
		Assert.assertThat(entity.getBlockTransfers().get(0).getId(), notNullValue());
	}
	//endregion

	// region retrieve
	@Test
	public void canReadSavedBlockUsingHeight() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 234, 0);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		final Block entity = this.blockDao.findByHeight(emptyBlock.getHeight());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getId()));
		Assert.assertThat(entity.getHeight(), equalTo(emptyBlock.getHeight().getRaw()));
		Assert.assertThat(entity.getBlockHash(), equalTo(HashUtils.calculateHash(emptyBlock)));
		Assert.assertThat(entity.getGenerationHash(), equalTo(emptyBlock.getGenerationHash()));
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
		Assert.assertThat(entity.getForger().getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getForgerProof(), equalTo(emptyBlock.getSignature().getBytes()));
	}

	@Test
	public void canReadSavedBlockUsingHash() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 345, 0);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		final Block entity = this.blockDao.findByHash(HashUtils.calculateHash(emptyBlock));

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getId()));
		Assert.assertThat(entity.getHeight(), equalTo(emptyBlock.getHeight().getRaw()));
		Assert.assertThat(entity.getBlockHash(), equalTo(HashUtils.calculateHash(emptyBlock)));
		Assert.assertThat(entity.getGenerationHash(), equalTo(emptyBlock.getGenerationHash()));
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
		Assert.assertThat(entity.getForger().getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getForgerProof(), equalTo(emptyBlock.getSignature().getBytes()));
	}

	@Test
	public void getBlocksForAccountDoesNotRetrieveTransfers() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456, 0);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		final Collection<Block> entities = this.blockDao.getBlocksForAccount(signer, null, 25);

		// Assert:
		Assert.assertThat(entities.size(), equalTo(1));
		final Block entity = entities.iterator().next();

		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getId()));
		ExceptionAssert.assertThrows(v -> entity.getBlockTransfers().size(), LazyInitializationException.class);
	}

	@Test
	public void getBlocksForAccountRespectsHash() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, 0);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			hashes.add(dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final Collection<Block> entities1 = this.blockDao.getBlocksForAccount(signer, hashes.get(29), 25);
		final Collection<Block> entities2 = this.blockDao.getBlocksForAccount(signer, hashes.get(0), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(1));
	}

	@Test
	public void getBlocksForAccountReturnsBlocksSortedByTime() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, (23 * i + 3) % 30);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			hashes.add(dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final Collection<Block> entities1 = this.blockDao.getBlocksForAccount(signer, hashes.get(29), 25);
		final Collection<Block> entities2 = this.blockDao.getBlocksForAccount(signer, hashes.get(29), 25);
		final Collection<Block> entities3 = this.blockDao.getBlocksForAccount(signer, hashes.get(0), 25);

		// Assert:
		final Consumer<Collection<Block>> assertCollectionContainsLatestBlocks = entities -> {
			Assert.assertThat(entities.size(), equalTo(25));

			int lastTimestamp = 123 + 29;
			for (final Block entity : entities) {
				Assert.assertThat(entity.getTimeStamp(), equalTo(lastTimestamp));
				lastTimestamp = lastTimestamp - 1;
			}
		};

		final Consumer<Collection<Block>> assertCollectionContainsFirstBlocks = entities -> {
			Assert.assertThat(entities.size(), equalTo(1));

			long height = 456;
			for (final Block entity : entities) {
				Assert.assertThat(entity.getHeight(), equalTo(height++));
			}
		};

		assertCollectionContainsLatestBlocks.accept(entities1);
		assertCollectionContainsLatestBlocks.accept(entities2);
		assertCollectionContainsFirstBlocks.accept(entities3);
	}
	//endregion

	//region delete/modify
	@Test
	public void deleteBlockDoesNotRemoveAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 567, 0);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		this.blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final org.nem.nis.dbmodel.Account entity = this.accountDao.getAccount(dbBlock.getForger().getId());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getForger().getId()));
		Assert.assertThat(entity.getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
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
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(dbBlock);
		this.blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final org.nem.nis.dbmodel.Account entity = this.accountDao.getAccount(dbBlock.getForger().getId());
		final Transfer transfer1 = this.transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(0)).getRaw());
		final Transfer transfer2 = this.transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(1)).getRaw());
		final Transfer transfer3 = this.transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(2)).getRaw());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getForger().getId()));
		Assert.assertThat(entity.getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
		Assert.assertThat(transfer1, nullValue());
		Assert.assertThat(transfer2, nullValue());
		Assert.assertThat(transfer3, nullValue());
	}
	//endregion

	//region getters
	@Test
	public void getHashesFromReturnsProperHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final ArrayList<Hash> expectedHashes = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedHashes.add(dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
		final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final Hash entity : entities1.asCollection()) {
			Assert.assertThat(entity, equalTo(expectedHashes.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getHashesFromReturnsHashesInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final TreeMap<Integer, Hash> expectedHashes = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			// mind that time is linear, so blocks are totally mixed when it comes to timestamp...
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedHashes.put((i * 23 + 3) % 30, dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final HashChain entities1 = this.blockDao.getHashesFrom(new BlockHeight(456), 25);
		final HashChain entities2 = this.blockDao.getHashesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final Hash entity : entities1.asCollection()) {
			Assert.assertThat(entity, equalTo(expectedHashes.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getDifficultiesFromReturnsProperDifficulties() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final ArrayList<Long> expectedDifficulties = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			emptyBlock.setDifficulty(new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + (i * 7000)));
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedDifficulties.add(dbBlock.getDifficulty());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
		final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final BlockDifficulty entity : entities1) {
			Assert.assertThat(entity.getRaw(), equalTo(expectedDifficulties.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getDifficultiesFromReturnsDifficultiesInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final TreeMap<Integer, Long> expectedDifficulties = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			emptyBlock.setDifficulty(new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + (i * 7000)));
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedDifficulties.put((i * 23 + 3) % 30, dbBlock.getDifficulty());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<BlockDifficulty> entities1 = this.blockDao.getDifficultiesFrom(new BlockHeight(456), 25);
		final List<BlockDifficulty> entities2 = this.blockDao.getDifficultiesFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final BlockDifficulty entity : entities1) {
			Assert.assertThat(entity.getRaw(), equalTo(expectedDifficulties.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getTimeStampsFromReturnsProperTimeStamps() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final ArrayList<Integer> expectedTimestamps = new ArrayList<>(30);
		for (int i = 0; i < 30; i++) {
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + i, i * 5);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedTimestamps.add(dbBlock.getTimeStamp());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
		final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final TimeInstant entity : entities1) {
			Assert.assertThat(entity.getRawTime(), equalTo(expectedTimestamps.get(i)));
			i = i + 1;
		}
	}

	@Test
	public void getTimeStampsFromReturnsTimeStampsInBlockHeightOrder() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, Utils.generateRandomAccount());

		// !!!
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final TreeMap<Integer, Integer> expectedTimeStamps = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			// mind that time is linear, so blocks are totally mixed when it comes to timestamp...
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 456 + (i * 23 + 3) % 30, i * 5);
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			expectedTimeStamps.put((i * 23 + 3) % 30, dbBlock.getTimeStamp());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final List<TimeInstant> entities1 = this.blockDao.getTimeStampsFrom(new BlockHeight(456), 25);
		final List<TimeInstant> entities2 = this.blockDao.getTimeStampsFrom(new BlockHeight(456 + 20), 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(10));

		int i = 0;
		for (final TimeInstant entity : entities1) {
			Assert.assertThat(entity.getRawTime(), equalTo(expectedTimeStamps.get(i)));
			i = i + 1;
		}
	}
	//endregion

	//region helpers
	private AccountDaoLookup prepareMapping(final Account sender, final Account recipient) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
		final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(recipient.getAddress().getEncoded(),
				recipient.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(sender, dbSender);
		mockAccountDao.addMapping(recipient, dbRecipient);
		return new AccountDaoLookupAdapter(mockAccountDao);
	}

	private org.nem.core.model.Block createTestEmptyBlock(final Account signer, final long height, final int i) {
		final Hash generationHash = HashUtils.nextHash(Hash.ZERO, signer.getKeyPair().getPublicKey());
		final org.nem.core.model.Block emptyBlock = new org.nem.core.model.Block(signer,
				Hash.ZERO,
				generationHash,
				new TimeInstant(123 + i),
				new BlockHeight(height));
		emptyBlock.sign();
		return emptyBlock;
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(0),
				sender,
				recipient,
				Amount.fromNem(amount),
				null
		);
		transferTransaction.sign();
		return transferTransaction;
	}
	//endregion
}
