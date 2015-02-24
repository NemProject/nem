package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
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
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountIoAdapterIntegrationTest {
	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Autowired
	SessionFactory sessionFactory;

	@After
	public void destroyDb() {
		final Session session = this.sessionFactory.openSession();
		DbUtils.dbCleanup(session);
		session.close();
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
		Assert.assertThat(result.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getAccountTransfersBorderCase() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = this.prepareAccountIoAdapter(recipient);

		// Act:
		final SerializableList<TransactionMetaDataPair> result = accountIoAdapter.getAccountTransfers(recipient.getAddress(), "123");

		// Assert:
		Assert.assertThat(result.size(), IsEqual.equalTo(1));
	}

	private static AccountIoAdapter createAccountIoAdapter(
			final ReadOnlyTransferDao transferDao,
			final ReadOnlyBlockDao blockDao,
			final ReadOnlyAccountCache accountCache) {
		return new AccountIoAdapter(transferDao, blockDao, accountCache, MapperUtils.createDbModelToModelNisMapper(accountCache));
	}

	// note: it would probably be better to mock blockDao and accountDao,
	// but I find this much easier (mostly stolen from TransferDaoTest)
	private AccountIoAdapter prepareAccountIoAdapter(final Account recipient) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account harvester = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(context.mockAccountDao);
		context.addMapping(harvester);

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

			context.addMapping(recipient);
			for (int i = 0; i < TX_COUNT; i++) {
				final Account randomSender = Utils.generateRandomAccount();
				context.addMapping(randomSender);
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(randomSender, recipient, 10, txTimes[blocks][i]);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		return createAccountIoAdapter(this.transferDao, this.blockDao, context.accountCache);
	}

	private void assertResultGetAccountTransfers(final SerializableList<TransactionMetaDataPair> result) {
		Assert.assertThat(result.size(), IsEqual.equalTo(25));

		// sorted by time...
		int i = 221;
		for (final TransactionMetaDataPair pair : result.asCollection()) {
			Assert.assertThat(pair.getTransaction().getTimeStamp().getRawTime(), IsEqual.equalTo(i));
			i -= 2;
		}

		// cause TXes are sorted by timestamp this is expected order
		final long[] expectedHeights = {
				19, 19, 19, 19, 18, 19, 18, 18, 18, 17, 18, 17, 17, 17, 17, 16, 16, 16, 16, 16, 15, 15, 15, 15, 15
		};
		i = 0;
		for (final TransactionMetaDataPair pair : result.asCollection()) {
			Assert.assertThat(pair.getMetaData().getHeight().getRaw(), IsEqual.equalTo(expectedHeights[i++]));
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

	private static class TestContext {
		private final MockAccountDao mockAccountDao = new MockAccountDao();
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);

		public void addMapping(final Account account) {
			final DbAccount dbSender = new DbAccount(account.getAddress());

			this.mockAccountDao.addMapping(account, dbSender);
			Mockito.when(this.accountCache.findByAddress(Mockito.eq(account.getAddress()), Mockito.any())).thenReturn(account);
			Mockito.when(this.accountCache.findByAddress(account.getAddress())).thenReturn(account);
		}
	}
}
