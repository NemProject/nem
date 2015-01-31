package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;

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
		final AccountIoAdapter accountIoAdapter = createAccountIoAdapter(null, null, accountCache);

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

	@Test
	public void findByAddressDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Account account = context.accountIoAdapter.findByAddress(context.address);

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(context.account));
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccount();
		context.seedDefaultTransactions();

		// Act:
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfers(context.address, "123");

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccount(context.account, 123, DEFAULT_LIMIT);
	}

	@Test
	public void getAccountTransfersDelegatesToTransferDaoWhenTimeStampIsNotSupplied() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccount();
		context.seedDefaultTransactions();

		// Act:
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfers(context.address, null);

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccount(context.account, Integer.MAX_VALUE, DEFAULT_LIMIT);
	}

	@Test
	public void getAccountTransfersDelegatesToTransferDaoWhenTimeStampIsNotParsable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccount();
		context.seedDefaultTransactions();

		// Act:
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfers(context.address, "NEM");

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccount(context.account, Integer.MAX_VALUE, DEFAULT_LIMIT);
	}

	@Test
	public void getAccountTransfersUsingHashDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccountUsingHash();
		context.seedDefaultTransactions();

		// Act:
		final Hash hash = Utils.generateRandomHash();
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfersUsingHash(
				context.address,
				hash,
				BlockHeight.ONE,
				ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingHash(
				context.account,
				hash,
				BlockHeight.ONE,
				ReadOnlyTransferDao.TransferType.ALL,
				DEFAULT_LIMIT);
	}

	@Test
	public void getAccountTransfersUsingIdDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccountUsingId();
		context.seedDefaultTransactions();

		// Act:
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfersUsingId(
				context.address,
				1234L,
				ReadOnlyTransferDao.TransferType.ALL);

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingId(
				context.account,
				1234L,
				ReadOnlyTransferDao.TransferType.ALL,
				DEFAULT_LIMIT);
	}

	@Test
	public void getAccountHarvestsDelegatesToBlockDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectBlocksForAccount();
		context.seedDefaultBlocks();

		// Act + Assert:
		final Hash hash = Utils.generateRandomHash();
		final SerializableList<HarvestInfo> harvestInfos = context.accountIoAdapter.getAccountHarvests(context.address, hash);

		// Assert:
		context.assertDefaultBlocks(harvestInfos);
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksForAccount(context.account, hash, DEFAULT_LIMIT);
	}

	private static class TestContext {
		private final AccountCache accountCache = mock(AccountCache.class);
		private final ReadOnlyBlockDao blockDao = mock(ReadOnlyBlockDao.class);
		private final ReadOnlyTransferDao transferDao = mock(ReadOnlyTransferDao.class);
		private final AccountIoAdapter accountIoAdapter = createAccountIoAdapter(this.transferDao, this.blockDao, this.accountCache);
		private final Account account = Utils.generateRandomAccount();
		private final Address address = this.account.getAddress();
		private final List<TransferBlockPair> pairs = new ArrayList<>();
		private final List<DbBlock> blocks = new ArrayList<>();

		public TestContext() {
			Mockito.when(this.accountCache.findByAddress(this.address)).thenReturn(this.account);
		}

		//region expect

		public void expectTransactionsForAccount() {
			Mockito.when(this.transferDao.getTransactionsForAccount(Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.pairs);
		}

		public void expectTransactionsForAccountUsingHash() {
			Mockito.when(this.transferDao.getTransactionsForAccountUsingHash(
					Mockito.any(),
					Mockito.any(),
					Mockito.any(),
					Mockito.any(),
					Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.pairs);
		}

		public void expectTransactionsForAccountUsingId() {
			Mockito.when(this.transferDao.getTransactionsForAccountUsingId(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.pairs);
		}

		public void expectBlocksForAccount() {
			Mockito.when(this.blockDao.getBlocksForAccount(Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.blocks);
		}

		//endregion

		//region seedDefaultTransactions / addTransaction

		public void seedDefaultTransactions() {
			this.addTransaction(12, 7, 111);
			this.addTransaction(12, 8, 222);
			this.addTransaction(15, 9, 333);
		}

		public void addTransaction(final int height, final int transactionId, final int amount) {
			final DbBlock block = NisUtils.createDbBlockWithTimeStampAtHeight(123, height);
			final DbTransferTransaction dbTransferTransaction = this.createTransfer(amount);
			dbTransferTransaction.setId((long)transactionId);
			this.pairs.add(new TransferBlockPair(dbTransferTransaction, block));
		}

		private DbTransferTransaction createTransfer(final int amount) {
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, signer, recipient, Amount.fromNem(amount), null);
			transaction.sign();

			this.addAccount(signer);
			this.addAccount(recipient);

			return MapperUtils.createModelToDbModelMapper(new MockAccountDao())
					.map(transaction, DbTransferTransaction.class);
		}

		private void addAccount(final Account account) {
			Mockito.when(this.accountCache.findByAddress(account.getAddress())).thenReturn(account);
		}

		//endregion

		//region assertDefaultTransactions

		public void assertDefaultTransactions(final SerializableList<TransactionMetaDataPair> pairs) {
			Assert.assertThat(pairs.size(), IsEqual.equalTo(3));
			this.assertHeights(pairs, 12L, 12L, 15L);
			this.assertIds(pairs, 7L, 8L, 9L);
			this.assertAmounts(pairs, 111L, 222L, 333L);
		}

		public void assertAmounts(final SerializableList<TransactionMetaDataPair> pairs, final Long... expectedAmounts) {
			final Collection<Long> amounts = pairs.asCollection().stream()
					.map(p -> ((TransferTransaction)p.getTransaction()).getAmount().getNumNem())
					.collect(Collectors.toList());
			Assert.assertThat(amounts, IsEquivalent.equivalentTo(expectedAmounts));
		}

		public void assertIds(final SerializableList<TransactionMetaDataPair> pairs, final Long... expectedIds) {
			final Collection<Long> ids = pairs.asCollection().stream()
					.map(p -> p.getMetaData().getId())
					.collect(Collectors.toList());
			Assert.assertThat(ids, IsEquivalent.equivalentTo(expectedIds));
		}

		public void assertHeights(final SerializableList<TransactionMetaDataPair> pairs, final Long... expectedHeights) {
			final Collection<Long> heights = pairs.asCollection().stream()
					.map(p -> p.getMetaData().getHeight().getRaw())
					.collect(Collectors.toList());
			Assert.assertThat(heights, IsEquivalent.equivalentTo(expectedHeights));
		}

		//endregion

		//region seedDefaultBlocks

		public void seedDefaultBlocks() {
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(897, 444));
			this.blocks.get(0).setTotalFee(8L);
			this.blocks.get(0).setBlockHash(Utils.generateRandomHash());
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(123, 777));
			this.blocks.get(1).setTotalFee(9L);
			this.blocks.get(1).setBlockHash(Utils.generateRandomHash());
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(345, 222));
			this.blocks.get(2).setTotalFee(7L);
			this.blocks.get(2).setBlockHash(Utils.generateRandomHash());
		}

		public void assertDefaultBlocks(final SerializableList<HarvestInfo> harvestInfos) {
			final Collection<Long> heights = harvestInfos.asCollection().stream()
					.map(p -> p.getBlockHeight().getRaw())
					.collect(Collectors.toList());
			Assert.assertThat(heights, IsEquivalent.equivalentTo(444L, 777L, 222L));

			final Collection<Integer> timeStamps = harvestInfos.asCollection().stream()
					.map(p -> p.getTimeStamp().getRawTime())
					.collect(Collectors.toList());
			Assert.assertThat(timeStamps, IsEquivalent.equivalentTo(897, 123, 345));

			final Collection<Long> fees = harvestInfos.asCollection().stream()
					.map(p -> p.getTotalFee().getNumMicroNem())
					.collect(Collectors.toList());
			Assert.assertThat(fees, IsEquivalent.equivalentTo(8L, 9L, 7L));

			final Collection<Hash> hashes = harvestInfos.asCollection().stream()
					.map(p -> p.getHash())
					.collect(Collectors.toList());
			Assert.assertThat(hashes, IsEquivalent.equivalentTo(this.blocks.stream().map(b -> b.getBlockHash()).collect(Collectors.toList())));
		}

		//endregion
	}

	// endregion

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
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}

		return createAccountIoAdapter(this.transferDao, this.blockDao, accountCache);
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
		final DbAccount dbSender = new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
		when(accountCache.findByAddress(account.getAddress())).thenReturn(account);
	}
}
