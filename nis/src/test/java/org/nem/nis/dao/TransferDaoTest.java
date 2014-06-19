package org.nem.nis.dao;

import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoTest {
	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void savingTransferSavesAccounts() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10);
		final Transfer entity = TransferMapper.toDbModel(transferTransaction, 0, accountDaoLookup);

		// Act
		transferDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getSender().getId(), notNullValue());
		Assert.assertThat(entity.getRecipient().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10);
		final Transfer dbTransfer = TransferMapper.toDbModel(transferTransaction, 12345, accountDaoLookup);

		// Act
		transferDao.save(dbTransfer);
		final Transfer entity = transferDao.findByHash(HashUtils.calculateHash(transferTransaction).getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransfer.getId()));
		Assert.assertThat(entity.getSender().getPublicKey(), equalTo(sender.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getAmount(), equalTo(transferTransaction.getAmount().getNumMicroNem()));
		Assert.assertThat(entity.getBlkIndex(), equalTo(12345));
		Assert.assertThat(entity.getSenderProof(), equalTo(transferTransaction.getSignature().getBytes()));
	}

	@Test
	public void getTransactionsForAccountRespectsTimestamp() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10);

		// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, transferTransaction.getTimeStamp(), BlockHeight.ONE);
		dummyBlock.addTransaction(transferTransaction);
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		blockDao.save(dbBlock);
		final Collection<Object[]> entities1 = transferDao.getTransactionsForAccount(sender, transferTransaction.getTimeStamp().getRawTime(), 25);
		final Collection<Object[]> entities2 = transferDao.getTransactionsForAccount(sender, transferTransaction.getTimeStamp().getRawTime()-1, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(1));
		Assert.assertThat(entities2.size(), equalTo(0));
	}

	private TransferTransaction prepareTransferTransaction(Account sender, Account recipient, long amount) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(123),
				sender,
				recipient,
				Amount.fromNem(amount),
				null
		);
		transferTransaction.sign();
		return transferTransaction;
	}

	private AccountDaoLookup prepareMapping(Account sender, Account recipient) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
		final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(recipient.getAddress().getEncoded(), recipient.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(sender, dbSender);
		mockAccountDao.addMapping(recipient, dbRecipient);
		return new AccountDaoLookupAdapter(mockAccountDao);
	}
}
