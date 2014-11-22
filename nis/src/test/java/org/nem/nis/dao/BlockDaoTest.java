package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
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
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.Entity;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;

import static org.hamcrest.core.IsEqual.equalTo;
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
	public void savingBlockSavesTransferTransactions() {
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
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(1));
		Assert.assertThat(entity.getBlockImportanceTransfers().size(), equalTo(0));
		Assert.assertThat(entity.getBlockTransfers().get(0).getId(), notNullValue());
	}

	@Test
	public void savingBlockSavesImportanceTransferTransactions() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, remote);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(signer, remote, true);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer, 133, 0);
		emptyBlock.addTransaction(transaction);
		emptyBlock.sign();
		final Block entity = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		this.blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
		Assert.assertThat(entity.getBlockImportanceTransfers().size(), equalTo(1));
		Assert.assertThat(entity.getBlockImportanceTransfers().get(0).getId(), notNullValue());
	}

	@Test
	public void savingBlockSavesTransactions() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, remote);
		final MultisigSignerModificationTransaction multisigSignerModificationTransaction = this.prepareMultisigSignerModificationTransaction(signer, remote);
		final ImportanceTransferTransaction importanceTransfer = this.prepareImportanceTransferTransaction(signer, remote, true);
		final TransferTransaction transferTransaction = this.prepareTransferTransaction(signer, remote, 10);
		final org.nem.core.model.Block block = this.createTestEmptyBlock(signer, 133, 0);
		block.addTransaction(multisigSignerModificationTransaction);
		block.addTransaction(importanceTransfer);
		block.addTransaction(transferTransaction);
		block.sign();

		final Block dbBlock = BlockMapper.toDbModel(block, accountDaoLookup);
		this.blockDao.save(dbBlock);

		// Act:
		final Block entity = this.blockDao.findByHeight(block.getHeight());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(1));
		Assert.assertThat(entity.getBlockImportanceTransfers().size(), equalTo(1));
		Assert.assertThat(entity.getBlockMultisigSignerModifications().size(), equalTo(1));
		Assert.assertThat(entity.getBlockTransfers().get(0).getId(), notNullValue());
		Assert.assertThat(entity.getBlockImportanceTransfers().get(0).getId(), notNullValue());
		Assert.assertThat(entity.getBlockMultisigSignerModifications().get(0).getId(), notNullValue());
	}

	// TODO 20141005 - since i imagine these tests will apply to all transaction types, it might make sense
	// > to refactor the validation and pass in transactions; although that might be hard, so it's not so important

	@Test
	public void savingDoesNotChangeImportanceTransferBlkIndex() {
		// Arrange:
		final Account signer1 = Utils.generateRandomAccount();
		final Account remote1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account remote2 = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer1, remote1, signer2, remote2);
		final ImportanceTransferTransaction importanceTransfer1 = this.prepareImportanceTransferTransaction(signer1, remote1, true);
		final ImportanceTransferTransaction importanceTransfer2 = this.prepareImportanceTransferTransaction(signer2, remote2, true);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer1, 133, 0);
		emptyBlock.addTransaction(importanceTransfer1);
		emptyBlock.addTransaction(importanceTransfer2);
		emptyBlock.sign();
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		dbBlock.getBlockImportanceTransfers().get(0).setBlkIndex(24);
		dbBlock.getBlockImportanceTransfers().get(1).setBlkIndex(12);

		this.blockDao.save(dbBlock);

		// Assert:
		Assert.assertThat(dbBlock.getBlockImportanceTransfers().get(0).getBlkIndex(), equalTo(24));
		Assert.assertThat(dbBlock.getBlockImportanceTransfers().get(1).getBlkIndex(), equalTo(12));
	}

	@Test
	public void savingChangesTransferTransactionOrderId() {
		// Arrange:
		final Account signer1 = Utils.generateRandomAccount();
		final Account remote1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account remote2 = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer1, remote1, signer2, remote2);
		final TransferTransaction transferTransaction1 = this.prepareTransferTransaction(signer1, remote1, 10);
		final TransferTransaction transferTransaction2 = this.prepareTransferTransaction(signer2, remote2, 10);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer1, 133, 0);
		emptyBlock.addTransaction(transferTransaction1);
		emptyBlock.addTransaction(transferTransaction2);
		emptyBlock.sign();
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		// TODO 20141010 J-G: i imagine you want to set the order id here
		dbBlock.getBlockTransfers().get(0).setBlkIndex(24);
		dbBlock.getBlockTransfers().get(1).setBlkIndex(12);

		this.blockDao.save(dbBlock);

		// Assert:
		// TODO 20141010 J-G: you don't need to revalidate getBlkIndex here; do you have a test like this for importance transfer?
		Assert.assertThat(dbBlock.getBlockTransfers().get(0).getOrderId(), equalTo(0));
		Assert.assertThat(dbBlock.getBlockTransfers().get(1).getOrderId(), equalTo(1));
		Assert.assertThat(dbBlock.getBlockTransfers().get(0).getBlkIndex(), equalTo(24));
		Assert.assertThat(dbBlock.getBlockTransfers().get(1).getBlkIndex(), equalTo(12));
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
	public void changingTransferTransactionBlkIndexDoesNotAffectOrderOfTxes() {
		// Arrange:
		final Account signer1 = Utils.generateRandomAccount();
		final Account remote1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account remote2 = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer1, remote1, signer2, remote2);
		final TransferTransaction transferTransaction1 = this.prepareTransferTransaction(signer1, remote1, 10);
		final TransferTransaction transferTransaction2 = this.prepareTransferTransaction(signer2, remote2, 10);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer1, 133, 0);
		emptyBlock.addTransaction(transferTransaction1);
		emptyBlock.addTransaction(transferTransaction2);
		emptyBlock.sign();
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		dbBlock.getBlockTransfers().get(0).setBlkIndex(24);
		dbBlock.getBlockTransfers().get(1).setBlkIndex(12);

		this.blockDao.save(dbBlock);
		final Block entity = this.blockDao.findByHash(HashUtils.calculateHash(emptyBlock));

		// Assert:
		Assert.assertThat(dbBlock.getBlockTransfers().get(0).getBlkIndex(), equalTo(24));
		Assert.assertThat(dbBlock.getBlockTransfers().get(1).getBlkIndex(), equalTo(12));
		Assert.assertThat(entity.getBlockTransfers().get(0).getBlkIndex(), equalTo(24));
		Assert.assertThat(entity.getBlockTransfers().get(1).getBlkIndex(), equalTo(12));
		// TODO 20151005 J-G i guess you're assuming the entity transactions are sorted?
		// > it might be better to check the hashes like in the following test
	}

	@Test
	public void changingImportanceTransferBlkIndexDoesNotAffectOrderOfTxes() {
		// Arrange:
		final Account signer1 = Utils.generateRandomAccount();
		final Account remote1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account remote2 = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer1, remote1, signer2, remote2);
		final ImportanceTransferTransaction importanceTransfer1 = this.prepareImportanceTransferTransaction(signer1, remote1, true);
		final ImportanceTransferTransaction importanceTransfer2 = this.prepareImportanceTransferTransaction(signer2, remote2, true);
		final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(signer1, 133, 0);
		emptyBlock.addTransaction(importanceTransfer1);
		emptyBlock.addTransaction(importanceTransfer2);
		emptyBlock.sign();
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
		dbBlock.getBlockImportanceTransfers().get(0).setBlkIndex(24);
		dbBlock.getBlockImportanceTransfers().get(1).setBlkIndex(12);

		this.blockDao.save(dbBlock);
		// Act:
		final Block entity = this.blockDao.findByHash(HashUtils.calculateHash(emptyBlock));

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
		Assert.assertThat(entity.getBlockImportanceTransfers().size(), equalTo(2));

		final Hash h1 = entity.getBlockImportanceTransfers().get(0).getTransferHash();
		final Hash h2 = entity.getBlockImportanceTransfers().get(1).getTransferHash();
		Assert.assertThat(entity.getBlockImportanceTransfers().get(0).getBlkIndex(), equalTo(24));
		Assert.assertThat(entity.getBlockImportanceTransfers().get(1).getBlkIndex(), equalTo(12));
		Assert.assertThat(h1, equalTo(HashUtils.calculateHash(importanceTransfer1)));
		Assert.assertThat(h2, equalTo(HashUtils.calculateHash(importanceTransfer2)));
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
	public void getBlocksForAccountReturnsBlockForagedViaRemoteAccount() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remoteAccount = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(signer, remoteAccount, Utils.generateRandomAccount());

		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			final Account blockSigner = (i % 2 == 0) ? signer : remoteAccount;
			final org.nem.core.model.Block emptyBlock = this.createTestEmptyBlock(blockSigner, 456 + i, 0);
			if (i % 2 == 1) {
				emptyBlock.setLessor(signer);
			}
			final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);
			hashes.add(dbBlock.getBlockHash());

			// Act:
			this.blockDao.save(dbBlock);
		}
		final Collection<Block> entities1 = this.blockDao.getBlocksForAccount(signer, hashes.get(29), 25);

		// Assert:
		// TODO 20151010 J-G can you add a comment explaining why 25 is expected?
		Assert.assertThat(entities1.size(), equalTo(25));
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
		Assert.assertThat(entities2.size(), equalTo(0));
	}

	@Test
	public void getBlocksForAccountReturnsBlocksSortedByHeight() {
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
		final Collection<Block> entities1 = this.blockDao.getBlocksForAccount(signer, null, 25);
		final Collection<Block> entities2 = this.blockDao.getBlocksForAccount(signer, hashes.get(29), 25);
		final Collection<Block> entities3 = this.blockDao.getBlocksForAccount(signer, hashes.get(0), 25);

		// Assert:
		final BiConsumer<Collection<Block>, Long> assertCollectionContainsBlocksStartingAtHeight = (entities, startHeight) -> {
			Assert.assertThat(entities.size(), equalTo(25));

			long lastHeight = startHeight;
			for (final Block entity : entities) {
				Assert.assertThat(entity.getHeight(), equalTo(lastHeight--));
			}
		};

		assertCollectionContainsBlocksStartingAtHeight.accept(entities1, 456L + 29);
		assertCollectionContainsBlocksStartingAtHeight.accept(entities2, 456L + 28);
		Assert.assertThat(entities3.size(), equalTo(0));
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

	@Test
	public void getBlocksAfterReturnsCorrectNumberOfBlocksIfEnoughBlocksAreAvailable() {
		// Arrange:
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);
		this.createBlocksInDatabase(10);

		// Act:
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksAfter(new BlockHeight(3), 5);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(5));
	}

	@Test
	public void getBlocksAfterReturnsAllBlocksAfterGivenHeightIfNotEnoughBlocksAreAvailable() {
		// Arrange:
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);
		this.createBlocksInDatabase(10);

		// Act:
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 15);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(7));
	}


	@Test
	public void getBlocksAfterReturnsBlocksWithTransactions() throws Exception {
		// Arrange:
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);
		this.createBlocksInDatabaseWithTransactions();

		// Act:
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksAfter(BlockHeight.ONE, 10);

		// Assert:
		Assert.assertThat(blocks.size(), IsEqual.equalTo(1));
	}

	@Test
	public void getBlocksAfterReturnsBlocksAfterGivenHeight() {
		// Arrange:
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);
		this.createBlocksInDatabase(10);

		// Act:
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 6);

		// Assert:
		Assert.assertThat(blocks.stream().findFirst().get().getHeight(), IsEqual.equalTo(3L));
	}

	@Test
	public void getBlocksAfterReturnsBlocksInAscendingOrderOfHeights() {
		// Arrange:
		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);
		this.createBlocksInDatabase(10);

		// Act:
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksAfter(new BlockHeight(2), 6);

		// Assert:
		org.nem.nis.dbmodel.Block previousBlock = null;
		for (final org.nem.nis.dbmodel.Block block : blocks) {
			if (null != previousBlock) {
				Assert.assertThat(previousBlock.getHeight(), IsEqual.equalTo(block.getHeight() - 1));
			}
			previousBlock = block;
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
			final org.nem.nis.dbmodel.Account dbA = new org.nem.nis.dbmodel.Account(address.getEncoded(), address.getPublicKey());
			mockAccountDao.addMapping(a, dbA);
		}
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

	private ImportanceTransferTransaction prepareImportanceTransferTransaction(final Account sender, final Account remote, final boolean isTransfer) {
		// Arrange:
		final ImportanceTransferTransaction importanceTransferTransaction = new ImportanceTransferTransaction(
				new TimeInstant(0),
				sender,
				isTransfer ? ImportanceTransferTransaction.Mode.Activate : ImportanceTransferTransaction.Mode.Deactivate,
				remote
		);
		importanceTransferTransaction.sign();
		return importanceTransferTransaction;
	}

	private MultisigSignerModificationTransaction prepareMultisigSignerModificationTransaction(
			final Account sender,
			final Account cosignatory
	) {
		// Arrange:
		final MultisigSignerModificationTransaction transaction = new MultisigSignerModificationTransaction(
				new TimeInstant(0),
				sender,
				MultisigSignerModificationTransaction.ModificationType.Add,
				cosignatory
		);
		transaction.sign();
		return transaction;
	}

	private List<Hash> createBlocksInDatabase(final int numBlocks) {
		final List<Hash> hashes = new ArrayList<>();
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);

		for (int i = 1; i < numBlocks; i++) {
			final org.nem.core.model.Block dummyBlock = new org.nem.core.model.Block(
					sender,
					Hash.ZERO,
					Hash.ZERO,
					new TimeInstant(i * 123),
					new BlockHeight(i));
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);
			this.blockDao.save(dbBlock);
		}

		return hashes;
	}

	private void createBlocksInDatabaseWithTransactions() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final List<Hash> hashes = new ArrayList<>();
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);

		final int numBlocks = 3;
		for (int i = 2; i < numBlocks; i++) {
			final org.nem.core.model.Block dummyBlock = new org.nem.core.model.Block(
					sender,
					Hash.ZERO,
					Hash.ZERO,
					new TimeInstant(i * 123),
					new BlockHeight(i));

			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

			final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
			scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
			// I wanted to make a generic test, that would create a dbBlock
			// with all possible transaction types, but idk how to do that
			/*
			for (final BeanDefinition beanDefinition : scanner.findCandidateComponents("org.nem.nis.dbmodel")) {
				final Class clazz = Class.forName(beanDefinition.getBeanClassName());
				final Type genericType = clazz.getGenericSuperclass();
				if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
					continue;
				}

				// create db-model transfer object
				final Object obj = clazz.newInstance();
				final List transaction = new ArrayList();
				transaction.add(obj);

				// call dbBlock.setBlock*
				final Method method = dbBlock.getClass().getMethod("setBlock" + clazz.getSimpleName() + "s", List.class);
				method.invoke(dbBlock, transaction);
			}
			*/

			this.blockDao.save(dbBlock);
		}
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
	}

	//endregion
}
