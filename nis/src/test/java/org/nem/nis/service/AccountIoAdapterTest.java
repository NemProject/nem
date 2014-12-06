package org.nem.nis.service;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountCache;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountIoAdapterTest {
	private static final int DEFAULT_LIMIT = 25;

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void AccountIoAdapterForwardsFindToAccountCache() {
		// Arrange:
		final AccountCache accountCache = mock(AccountCache.class);
		final Vector<Account> accounts = new Vector<>();
		for (int i = 0; i < 10; ++i) {
			final Account account = Utils.generateRandomAccount();
			accounts.add(account);
			when(accountCache.findByAddress(account.getAddress())).thenReturn(account);
		}
		final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(null, null, accountCache);

		// Assert:
		for (int i = 0; i < 10; ++i) {
			Assert.assertThat(accountIoAdapter.findByAddress(accounts.get(i).getAddress()), equalTo(accounts.get(i)));
		}
	}

	@Test
	public void getAccountTransfersReturnsTransfersSortedByTimestamp() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = this.prepareAccountIoAdapter(recipient);

		// Act:
		final SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "223");

		// Assert:
		this.assertResultGetAccountTransfers(result);
	}

	@Test
	public void getAccountTransfersReturnsEmptyListWhenTimestampIsInPast() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = this.prepareAccountIoAdapter(recipient);

		// Act:
		final SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "0");

		// Assert:
		Assert.assertThat(result.size(), equalTo(0));
	}

	@Test
	public void getAccountTransfersBorderCase() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = this.prepareAccountIoAdapter(recipient);

		// Act:
		final SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "123");

		// Assert:
		Assert.assertThat(result.size(), equalTo(1));
	}

	// region delegation

	// TODO 20141205 J-B; should we also validate return values?

	@Test
	public void findByAddressDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.findByAddress(context.address);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfers(context.address, "123");

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfers(context.address, "123");

		// Assert:
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccount(
				Mockito.any(),
				Mockito.any(),
				Mockito.eq(DEFAULT_LIMIT));
	}

	@Test
	public void getAccountTransfersUsingHashDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfersUsingHash(
				context.address,
				Utils.generateRandomHash(),
				BlockHeight.ONE,
				ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersUsingHashDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfersUsingHash(
				context.address,
				Utils.generateRandomHash(),
				BlockHeight.ONE,
				ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingHash(
				Mockito.any(),
				Mockito.any(),
				Mockito.eq(BlockHeight.ONE),
				Mockito.eq(ReadOnlyTransferDao.TransferType.ALL),
				Mockito.eq(DEFAULT_LIMIT));
	}

	@Test
	public void getAccountTransfersUsingIdDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfersUsingId(context.address, 1234L, ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersUsingIdDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountTransfersUsingId(context.address, 1234L, ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingId(
				Mockito.any(),
				Mockito.eq(1234L),
				Mockito.eq(ReadOnlyTransferDao.TransferType.ALL),
				Mockito.eq(DEFAULT_LIMIT));
	}

	@Test
	public void getAccountHarvestsDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountHarvests(context.address, Utils.generateRandomHash());

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountHarvestsDelegatesToBlockDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.accountIoAdapter.getAccountHarvests(context.address, Utils.generateRandomHash());

		// Assert:
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksForAccount(
				Mockito.any(),
				Mockito.any(),
				Mockito.eq(DEFAULT_LIMIT));
	}

	private class TestContext {
		private final AccountCache accountCache = mock(AccountCache.class);
		private final ReadOnlyBlockDao blockDao = mock(ReadOnlyBlockDao.class);
		private final ReadOnlyTransferDao transferDao = mock(ReadOnlyTransferDao.class);
		private final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(transferDao, blockDao, accountCache);
		private final Address address = Utils.generateRandomAddress();

		public TestContext() {
			Mockito.when(this.accountCache.findByAddress(Mockito.any())).thenReturn(Utils.generateRandomAccount());
			Mockito.when(this.transferDao.getTransactionsForAccount(Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(new ArrayList<>());
			Mockito.when(this.transferDao.getTransactionsForAccountUsingHash(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(new ArrayList<>());
			Mockito.when(this.transferDao.getTransactionsForAccountUsingId(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(new ArrayList<>());
		}
	}

	// endregion

	// note: it would probably be better to mock blockDao and accountDao,
	// but I find this much easier (mostly stolen from TransferDaoTest)
	private AccountIoAdapter prepareAccountIoAdapter(final Account recipient) {
		// Arrange:
		final Account harvester = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		final AccountCache accountCache = mock(AccountCache.class);
		this.addMapping(accountCache, mockAccountDao, harvester);

		this.blockDao.deleteBlocksAfterHeight(BlockHeight.ONE);

		final int TX_COUNT = 5;
		// generated and put here, to make manipulations easier
		final int[] blockTimes = { 133, 143, 153, 163, 173, 183, 193, 203, 212, 223 };
		final int[][] txTimes = {
				{ 123, 125, 127, 129, 131 },
				{ 133, 135, 137, 139, 141 },
				{ 143, 145, 147, 149, 151 },
				{ 153, 155, 157, 159, 161 },
				{ 163, 165, 167, 169, 171 },
				{ 173, 175, 177, 179, 181 },
				{ 183, 185, 187, 189, 191 },
				{ 193, 195, 197, 199, 203 }, // change tx timestamps a bit
				{ 201, 205, 207, 209, 213 }, //
				{ 211, 215, 217, 219, 221 } //
		};

		for (int blocks = 0; blocks < 10; ++blocks) {
			final Block dummyBlock = new Block(harvester, Hash.ZERO, Hash.ZERO, new TimeInstant(blockTimes[blocks]), new BlockHeight(10 + blocks));

			this.addMapping(accountCache, mockAccountDao, recipient);
			for (int i = 0; i < TX_COUNT; i++) {
				final Account randomSender = Utils.generateRandomAccount();
				this.addMapping(accountCache, mockAccountDao, randomSender);
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(randomSender, recipient, 10, txTimes[blocks][i]);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		return new AccountIoAdapter(this.transferDao, this.blockDao, accountCache);
	}

	private void assertResultGetAccountTransfers(final SerializableList<TransactionMetaDataPair> result) {
		Assert.assertThat(result.size(), equalTo(25));

		// sorted by time...
		int i = 221;
		for (final TransactionMetaDataPair pair : result.asCollection()) {
			Assert.assertThat(pair.getTransaction().getTimeStamp().getRawTime(), equalTo(i));
			i -= 2;
		}

		// cause TXes are sorted by timestamp this is expected order
		final long[] expectedHeights = {
				19, 19, 19, 19, 18, 19, 18, 18, 18, 17, 18, 17, 17, 17, 17, 16, 16, 16, 16, 16, 15, 15, 15, 15, 15
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

	private void addMapping(final AccountCache accountCache, final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
		when(accountCache.findByAddress(account.getAddress())).thenReturn(account);
	}
}
