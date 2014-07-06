package org.nem.nis.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.TransferTransaction;
import org.nem.core.model.ncc.TransactionMetaData;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TestConf;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Vector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountIoAdapterTest {
	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void AccountIoAdapterForwardsFindToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = mock(AccountAnalyzer.class);
		final Vector<Account> accounts = new Vector<>();
		for (int i = 0; i < 10; ++i) {
			final Account account = Utils.generateRandomAccount();
			accounts.add(account);
			when(accountAnalyzer.findByAddress(account.getAddress())).thenReturn(account);
		}
		final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(null, null, accountAnalyzer);

		// Assert:
		for (int i = 0; i < 10; ++i) {
			Assert.assertThat(accountIoAdapter.findByAddress(accounts.get(i).getAddress()), equalTo(accounts.get(i)));
		}
	}

	@Test
	public void getAccountTransfersReturnsTransfersSortedByTimestamp() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = prepareAccountIoAdapter(recipient);

		// Act:
		SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "223");

		// Assert:
		assertResultGetAccountTransfers(result);
	}


	@Test
	public void getAccountTransfersReturnsEmptyListWhenTimestampIsInPast() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = prepareAccountIoAdapter(recipient);

		// Act:
		SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "0");

		// Assert:
		Assert.assertThat(result.size(), equalTo(0));
	}


	@Test
	public void getAccountTransfersBorderCase() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = prepareAccountIoAdapter(recipient);

		// Act:
		SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "123");

		// Assert:
		Assert.assertThat(result.size(), equalTo(1));
	}

	// note: it would probably be better to mock blockDao and accountDao,
	// but I find this much easier (mostly stolen from TransferDaoTest)
	private AccountIoAdapter prepareAccountIoAdapter(final Account recipient) {
		// Arrange:
		final Account harvester = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		final AccountAnalyzer accountAnalyzer = mock(AccountAnalyzer.class);
		addMapping(accountAnalyzer, mockAccountDao, harvester);

		blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final int TX_COUNT = 5;
		// generated and put here, to make manipulations easier
		final int[] blockTimes = { 133, 143, 153, 163, 173,  183, 193, 203, 212, 223 };
		final int[][] txTimes = {
				{123, 125, 127, 129, 131},
				{133, 135, 137, 139, 141},
				{143, 145, 147, 149, 151},
				{153, 155, 157, 159, 161},
				{163, 165, 167, 169, 171},
				{173, 175, 177, 179, 181},
				{183, 185, 187, 189, 191},
				{193, 195, 197, 199, 203}, // change tx timestamps a bit
				{201, 205, 207, 209, 213}, //
				{211, 215, 217, 219, 221} //
		};

		for (int blocks = 0; blocks < 10; ++blocks) {
			final Block dummyBlock = new Block(harvester, Hash.ZERO, Hash.ZERO, new TimeInstant(blockTimes[blocks]), new BlockHeight(10 + blocks));

			addMapping(accountAnalyzer, mockAccountDao, recipient);
			for (int i = 0; i < TX_COUNT; i++) {
				final Account randomSender = Utils.generateRandomAccount();
				addMapping(accountAnalyzer, mockAccountDao, randomSender);
				final TransferTransaction transferTransaction = prepareTransferTransaction(randomSender, recipient, 10, txTimes[blocks][i]);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		return new AccountIoAdapter(new RequiredTransferDaoAdapter(transferDao), new RequiredBlockDaoAdapter(blockDao), accountAnalyzer);
	}


	private void assertResultGetAccountTransfers(SerializableList<TransactionMetaDataPair> result) {
		Assert.assertThat(result.size(), equalTo(25));

		// sorted by time...
		int i = 221;
		for (final TransactionMetaDataPair pair : result.asCollection()) {
			Assert.assertThat(pair.getTransaction().getTimeStamp().getRawTime(), equalTo(i));
			i -= 2;
		}

		// cause TXes are sorted by timestamp this is expected order
		long[] expectedHeights = {
				19,19,19,19,18,  19,18,18,18,17, 18,17,17,17,17, 16,16,16,16,16, 15,15,15,15,15
		};
		i = 0;
		for (final TransactionMetaDataPair pair : result.asCollection()) {
			Assert.assertThat(pair.getMetaData().getHeight().getRaw(), equalTo(expectedHeights[i++]));
		}
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount, final int i) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(i),
				sender,
				recipient,
				Amount.fromNem(amount),
				null
		);
		transferTransaction.sign();
		return transferTransaction;
	}

	private void addMapping(AccountAnalyzer accountAnalyzer, final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
		when(accountAnalyzer.findByAddress(account.getAddress())).thenReturn(account);
	}
}
