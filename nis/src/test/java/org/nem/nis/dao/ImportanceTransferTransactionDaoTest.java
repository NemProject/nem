package org.nem.nis.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.ImportanceTransferMapper;
import org.nem.nis.mappers.TransferMapper;
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
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransactionDirection.Revert);
		final ImportanceTransfer entity = ImportanceTransferMapper.toDbModel(transaction, 0, accountDaoLookup);

		// Act
		this.importanceTransferDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getSender().getId(), notNullValue());
		Assert.assertThat(entity.getRemote().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransactionDirection.Revert);
		final ImportanceTransfer dbTransaction = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);

		// Act
		this.importanceTransferDao.save(dbTransaction);
		final ImportanceTransfer entity = this.importanceTransferDao.findByHash(HashUtils.calculateHash(transaction).getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransaction.getId()));
		Assert.assertThat(entity.getSender().getPublicKey(), equalTo(sender.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRemote().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getDirection(), equalTo(transaction.getDirection()));
		Assert.assertThat(entity.getBlkIndex(), equalTo(12345));
		Assert.assertThat(entity.getSenderProof(), equalTo(transaction.getSignature().getBytes()));
	}

	@Test
	public void countReturnsProperValue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final ImportanceTransferTransaction transaction = this.prepareImportanceTransferTransaction(sender, recipient, 123, ImportanceTransferTransactionDirection.Revert);
		final ImportanceTransfer dbTransfer1 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final ImportanceTransfer dbTransfer2 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final ImportanceTransfer dbTransfer3 = ImportanceTransferMapper.toDbModel(transaction, 12345, accountDaoLookup);
		final Long initialCount = this.importanceTransferDao.count();

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

	private ImportanceTransferTransaction prepareImportanceTransferTransaction(final Account sender, final Account recipient, final int i, final int mode) {
		// Arrange:
		final ImportanceTransferTransaction transferTransaction = new ImportanceTransferTransaction(
				new TimeInstant(i),
				sender,
				mode,
				recipient.getAddress());
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
