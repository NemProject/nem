package org.nem.nis.dao;

import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
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
		final TestContext testContext = new TestContext();
		final ImportanceTransfer dbTransaction = testContext.createDbTransaction();

		final org.nem.nis.dbmodel.Account account = testContext.getSender();
		this.addToDummyBlock(account, dbTransaction);

		// Act
		this.importanceTransferDao.save(dbTransaction);

		// Assert:
		Assert.assertThat(dbTransaction.getId(), notNullValue());
		Assert.assertThat(dbTransaction.getSender().getId(), notNullValue());
		Assert.assertThat(dbTransaction.getRemote().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// Arrange:
		final TestContext testContext = new TestContext();
		final ImportanceTransfer dbTransaction = testContext.createDbTransaction();

		final org.nem.nis.dbmodel.Account account = testContext.getSender();
		this.addToDummyBlock(account, dbTransaction);

		// Act
		this.importanceTransferDao.save(dbTransaction);
		final ImportanceTransfer entity = this.importanceTransferDao.findByHash(testContext.getTransactionHash().getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransaction.getId()));

		testContext.assertTransaction(entity);
	}

	@Test
	public void countReturnsProperValue() {
		// Arrange:
		final TestContext testContext = new TestContext();
		final ImportanceTransfer dbTransfer1 = testContext.createDbTransaction();
		final ImportanceTransfer dbTransfer2 = testContext.createDbTransaction();
		final ImportanceTransfer dbTransfer3 = testContext.createDbTransaction();
		final Long initialCount = this.importanceTransferDao.count();

		final org.nem.nis.dbmodel.Account account = testContext.getSender();
		this.addToDummyBlock(account, dbTransfer1, dbTransfer2, dbTransfer3);

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

	private void addToDummyBlock(final org.nem.nis.dbmodel.Account account, final ImportanceTransfer... dbTransfers) {
		final Block block = NisUtils.createDummyDbBlock(account);
		this.blockDao.save(block);

		for (final ImportanceTransfer importanceTransfer : dbTransfers) {
			importanceTransfer.setBlock(block);
		}
	}

	private class TestContext {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup;
		final ImportanceTransferTransaction transaction;

		TestContext() {
			this.accountDaoLookup = this.prepareMapping(this.sender, this.recipient);
			this.transaction = this.prepareImportanceTransferTransaction(this.sender, this.recipient, 123, ImportanceTransferTransaction.Mode.Deactivate);
		}

		private AccountDaoLookup prepareMapping(final Account sender, final Account recipient) {
			// Arrange:
			final MockAccountDao mockAccountDao = new MockAccountDao();
			final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getAddress().getPublicKey());
			final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(
					recipient.getAddress().getEncoded(),
					recipient.getAddress().getPublicKey());
			mockAccountDao.addMapping(sender, dbSender);
			mockAccountDao.addMapping(recipient, dbRecipient);
			return new AccountDaoLookupAdapter(mockAccountDao);
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

		public ImportanceTransfer createDbTransaction() {
			return ImportanceTransferMapper.toDbModel(this.transaction, 12345, 0, this.accountDaoLookup);
		}

		public org.nem.nis.dbmodel.Account getSender() {
			return this.accountDaoLookup.findByAddress(this.sender.getAddress());
		}

		public Hash getTransactionHash() {
			return HashUtils.calculateHash(this.transaction);
		}

		public void assertTransaction(final ImportanceTransfer entity) {
			Assert.assertThat(entity.getSender().getPublicKey(), equalTo(this.sender.getAddress().getPublicKey()));
			Assert.assertThat(entity.getRemote().getPublicKey(), equalTo(this.recipient.getAddress().getPublicKey()));
			Assert.assertThat(entity.getMode(), equalTo(this.transaction.getMode().value()));
			Assert.assertThat(entity.getSenderProof(), equalTo(this.transaction.getSignature().getBytes()));
		}
	}
}
