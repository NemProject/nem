package org.nem.nis.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.ImportanceTransferMapper;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ImportanceTransferTransactionDaoTest {
	@Autowired
	ImportanceTransferDao importanceTransferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void savingTransferSavesAccounts() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransaction.Mode.Deactivate);
		final ImportanceTransfer entity = ImportanceTransferMapper.toDbModel(transaction, 0, accountDaoLookup);

		final org.nem.nis.dbmodel.Account account = accountDaoLookup.findByAddress(sender.getAddress());
		addToDummyBlock(account, entity);

		// Act
		this.importanceTransferDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getSender().getId(), notNullValue());
		Assert.assertThat(entity.getRemote().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// TODO-CR 20140919 J-G: comment - since the arrange block is mostly the same, consider using a private "TestContext" helper class to reduce duplication
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransaction.Mode.Deactivate);
		final ImportanceTransfer dbTransaction = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);

		final org.nem.nis.dbmodel.Account account = accountDaoLookup.findByAddress(sender.getAddress());
		addToDummyBlock(account, dbTransaction);

		// Act
		this.importanceTransferDao.save(dbTransaction);
		final ImportanceTransfer entity = this.importanceTransferDao.findByHash(HashUtils.calculateHash(transaction).getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransaction.getId()));
		Assert.assertThat(entity.getSender().getPublicKey(), equalTo(sender.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRemote().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getDirection(), equalTo(transaction.getMode().value()));
		// TODO-CR 20140919 J-G: commented out code did you mean to remove this or verify it?
		//Assert.assertThat(entity.getBlkIndex(), equalTo(12345));
		Assert.assertThat(entity.getSenderProof(), equalTo(transaction.getSignature().getBytes()));
	}

	@Test
	public void countReturnsProperValue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransaction.Mode.Deactivate);
		final ImportanceTransfer dbTransfer1 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final ImportanceTransfer dbTransfer2 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final ImportanceTransfer dbTransfer3 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final Long initialCount = this.importanceTransferDao.count();

		final org.nem.nis.dbmodel.Account account = accountDaoLookup.findByAddress(sender.getAddress());
		addToDummyBlock(account, dbTransfer1, dbTransfer2, dbTransfer3);

		// Act
		this.importanceTransferDao.save(dbTransfer1);
		final Long count1 = this.importanceTransferDao.count();
		this.importanceTransferDao.save(dbTransfer2);
		final Long count2 = this.importanceTransferDao.count();
		this.importanceTransferDao.save(dbTransfer3);
		final Long count3 = this.importanceTransferDao.count();

		// Assert:
		Assert.assertThat(count1, equalTo(initialCount + 1));
		Assert.assertThat(count2, equalTo(initialCount + 2));
		Assert.assertThat(count3, equalTo(initialCount + 3));
	}

	private void addToDummyBlock(final org.nem.nis.dbmodel.Account account, ImportanceTransfer... dbTransfers) {
		final Block block = new Block(Hash.ZERO,1, Hash.ZERO, Hash.ZERO, 1,
				account, new byte[]{1,2,3,4},
				1L, 1L, 1L, 123L);
		this.blockDao.save(block);

		for (final ImportanceTransfer importanceTransfer : dbTransfers) {
			importanceTransfer.setBlock(block);
		}
	}

	private ImportanceTransferTransaction prepareImportanceTransferTransaction(
			final Account sender,
			final Account recipient,
			final int i,
			final ImportanceTransferTransaction.Mode mode) {
		// Arrange:
		final ImportanceTransferTransaction transferTransaction = new ImportanceTransferTransaction(
				new TimeInstant(i),
				sender,
				mode,
				recipient);
		transferTransaction.sign();
		return transferTransaction;
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
}
