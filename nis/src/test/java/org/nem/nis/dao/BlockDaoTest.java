package org.nem.nis.dao;

import org.hibernate.LazyInitializationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;
import org.nem.core.model.Hash;
import org.nem.core.model.HashUtils;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class BlockDaoTest {
	@Autowired
	AccountDao accountDao;

	@Autowired
	BlockDao blockDao;

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
		final Collection<Block> entities = blockDao.getBlocksForAccount(signer, 25);

		// Assert:
		Assert.assertThat(entities.size(), equalTo(1));
		final Block entity = entities.iterator().next();

		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getId()));
		Assert.assertThat(entity.getBlockTransfers().size(), equalTo(0));
	}

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
		org.nem.nis.dbmodel.Account entity = accountDao.getAccount(dbBlock.getForger().getId());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbBlock.getForger().getId()));
		Assert.assertThat(entity.getPublicKey(), equalTo(signer.getKeyPair().getPublicKey()));
	}

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
}
