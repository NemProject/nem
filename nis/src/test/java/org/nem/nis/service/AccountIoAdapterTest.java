package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.AccountCache;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AccountIoAdapterTest {
	private static final int DEFAULT_LIMIT = 25;

	@Test
	public void AccountIoAdapterForwardsFindToAccountCache() {
		// Arrange:
		final AccountCache accountCache = Mockito.mock(AccountCache.class);
		final Vector<Account> accounts = new Vector<>();
		for (int i = 0; i < 10; ++i) {
			final Account account = Utils.generateRandomAccount();
			accounts.add(account);
			Mockito.when(accountCache.findByAddress(account.getAddress())).thenReturn(account);
		}
		final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(null, null, accountCache, Mockito.mock(NisDbModelToModelMapper.class));

		// Assert:
		for (int i = 0; i < 10; ++i) {
			Assert.assertThat(accountIoAdapter.findByAddress(accounts.get(i).getAddress()), IsEqual.equalTo(accounts.get(i)));
		}
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
		final Long id = Utils.generateRandomId();
		final SerializableList<HarvestInfo> harvestInfos = context.accountIoAdapter.getAccountHarvests(context.address, id);

		// Assert:
		context.assertDefaultBlocks(harvestInfos);
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksForAccount(context.account, id, DEFAULT_LIMIT);
	}

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final ReadOnlyTransferDao transferDao = Mockito.mock(ReadOnlyTransferDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(
				this.transferDao,
				this.blockDao,
				this.accountCache,
				this.mapper);
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

			final DbTransferTransaction dbTransferTransaction = new DbTransferTransaction();
			Mockito.when(this.mapper.map(dbTransferTransaction)).thenReturn(transaction);
			return dbTransferTransaction;
		}

		//endregion

		//region assertDefaultTransactions

		public void assertDefaultTransactions(final SerializableList<TransactionMetaDataPair> pairs) {
			Assert.assertThat(pairs.size(), IsEqual.equalTo(3));
			this.assertHeights(pairs, 12L, 12L, 15L);
			this.assertIds(pairs, 7L, 8L, 9L);
			this.assertAmounts(pairs, 111L, 222L, 333L);
			Mockito.verify(this.mapper, Mockito.times(3)).map(Mockito.any(AbstractBlockTransfer.class));
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
}
