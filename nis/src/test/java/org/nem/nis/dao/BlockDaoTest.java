package org.nem.nis.dao;

import org.hibernate.LazyInitializationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

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
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 123);
		final Block entity = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getForger().getId(), notNullValue());
	}

	@Test
	public void savingBlockSavesTransactions() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(recipient, signer, 10);
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 133);
		emptyBlock.addTransaction(transferTransaction);
		emptyBlock.sign();
		final Block entity = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(entity);

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
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 234);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		final Block entity = blockDao.findByHeight(emptyBlock.getHeight());

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
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 345);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		final Block entity = blockDao.findByHash(HashUtils.calculateHash(emptyBlock));

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

	@Test(expected = LazyInitializationException.class)
	public void getBlocksForAccountDoesNotRetrieveTransfers() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 456);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		final Collection<Block> entities = blockDao.getBlocksForAccount(signer, Integer.MAX_VALUE, 25);

		// Assert:
		Assert.assertThat(entities.size(), equalTo(1));
		final Block entity = entities.iterator().next();

		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getId()));
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
	}

	@Test
	public void getBlocksForAccountRespectsTimestamp() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 456);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		final Collection<Block> entities1 = blockDao.getBlocksForAccount(signer, emptyBlock.getTimeStamp().getRawTime(), 25);
		final Collection<Block> entities2 = blockDao.getBlocksForAccount(signer, emptyBlock.getTimeStamp().getRawTime()-1, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(1));
		Assert.assertThat(entities2.size(), equalTo(0));
	}
	//endregion

	//region delete/modify
	@Test
	public void deleteBlockDoesNotRemoveAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, Utils.generateRandomAccount());
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 567);
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final org.nem.nis.dbmodel.Account entity = accountDao.getAccount(dbBlock.getForger().getId());

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
		final AccountDaoLookup accountDaoLookup = prepareMapping(signer, recipient);
		final org.nem.core.model.Block emptyBlock = createTestEmptyBlock(signer, 678);
		emptyBlock.addTransaction(prepareTransferTransaction(signer, recipient, 10));
		emptyBlock.addTransaction(prepareTransferTransaction(signer, recipient, 20));
		emptyBlock.addTransaction(prepareTransferTransaction(signer, recipient, 30));
		emptyBlock.sign();
		final Block dbBlock = BlockMapper.toDbModel(emptyBlock, accountDaoLookup);

		// Act:
		blockDao.save(dbBlock);
		blockDao.deleteBlocksAfterHeight(emptyBlock.getHeight().prev());
		final org.nem.nis.dbmodel.Account entity = accountDao.getAccount(dbBlock.getForger().getId());
		final Transfer transfer1 = transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(0)).getRaw());
		final Transfer transfer2 = transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(1)).getRaw());
		final Transfer transfer3 = transferDao.findByHash(HashUtils.calculateHash(emptyBlock.getTransactions().get(2)).getRaw());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getForger().getId()));
		Assert.assertThat(entity.getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
		Assert.assertThat(transfer1, nullValue());
		Assert.assertThat(transfer2, nullValue());
		Assert.assertThat(transfer3, nullValue());
	}
	//endregion

	//region helpers
	private AccountDaoLookup prepareMapping(final Account sender, final Account recipient) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
		final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(recipient.getAddress().getEncoded(), recipient.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(sender, dbSender);
		mockAccountDao.addMapping(recipient, dbRecipient);
		return new AccountDaoLookupAdapter(mockAccountDao);
	}

	private org.nem.core.model.Block createTestEmptyBlock(final Account signer, long height) {
		final Hash generationHash = HashUtils.nextHash(Hash.ZERO, signer.getKeyPair().getPublicKey());
		final org.nem.core.model.Block emptyBlock = new org.nem.core.model.Block(signer, Hash.ZERO, generationHash, new TimeInstant(123), new BlockHeight(height));
		emptyBlock.sign();
		return emptyBlock;
	}

	private TransferTransaction prepareTransferTransaction(Account sender, Account recipient, long amount) {
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
