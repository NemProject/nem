package org.nem.nis.service;

import org.hamcrest.MatcherAssert;
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

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class AccountIoAdapterTest {

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
		final AccountIoAdapter accountIoAdapter = createAccountIoAdapter(accountCache);

		// Assert:
		for (int i = 0; i < 10; ++i) {
			MatcherAssert.assertThat(accountIoAdapter.findByAddress(accounts.get(i).getAddress()), IsEqual.equalTo(accounts.get(i)));
		}
	}

	private static AccountIoAdapter createAccountIoAdapter(final AccountCache accountCache) {
		return new AccountIoAdapter(null, null, accountCache, Mockito.mock(NisDbModelToModelMapper.class));
	}

	@Test
	public void findByAddressDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Account account = context.accountIoAdapter.findByAddress(context.address);

		// Assert:
		MatcherAssert.assertThat(account, IsEqual.equalTo(context.account));
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(context.address);
	}

	@Test
	public void getAccountTransfersUsingHashDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccountUsingHash();
		context.seedDefaultTransactions();

		// Act:
		final Hash hash = Utils.generateRandomHash();
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfersUsingHash(context.address, hash,
				BlockHeight.ONE, ReadOnlyTransferDao.TransferType.ALL, 30);

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingHash(context.account, hash, BlockHeight.ONE,
				ReadOnlyTransferDao.TransferType.ALL, 30);
	}

	@Test
	public void getAccountTransfersUsingIdDelegatesToTransferDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectTransactionsForAccountUsingId();
		context.seedDefaultTransactions();

		// Act:
		final SerializableList<TransactionMetaDataPair> pairs = context.accountIoAdapter.getAccountTransfersUsingId(context.address, 1234L,
				ReadOnlyTransferDao.TransferType.ALL, 35);

		// Assert:
		context.assertDefaultTransactions(pairs);
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionsForAccountUsingId(context.account, 1234L,
				ReadOnlyTransferDao.TransferType.ALL, 35);
	}

	@Test
	public void getAccountHarvestsDelegatesToBlockDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectBlocksForAccount();
		context.seedDefaultBlocks();

		// Act + Assert:
		final Long id = Utils.generateRandomId();
		final SerializableList<HarvestInfo> harvestInfos = context.accountIoAdapter.getAccountHarvests(context.address, id, 40);

		// Assert:
		context.assertDefaultBlocks(harvestInfos);
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksForAccount(context.account, id, 40);
	}

	@SuppressWarnings("serial")
	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final ReadOnlyTransferDao transferDao = Mockito.mock(ReadOnlyTransferDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(this.transferDao, this.blockDao, this.accountCache,
				this.mapper);
		private final Account account = Utils.generateRandomAccount();
		private final Address address = this.account.getAddress();
		private final List<TransferBlockPair> pairs = new ArrayList<>();
		private final List<DbBlock> blocks = new ArrayList<>();
		private final List<Hash> transactionHashes = new ArrayList<Hash>() {
			{
				this.add(Utils.generateRandomHash());
				this.add(Utils.generateRandomHash());
				this.add(Utils.generateRandomHash());
			}
		};

		public TestContext() {
			Mockito.when(this.accountCache.findByAddress(this.address)).thenReturn(this.account);
		}

		// region expect

		public void expectTransactionsForAccountUsingHash() {
			Mockito.when(this.transferDao.getTransactionsForAccountUsingHash(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyInt())).thenReturn(this.pairs);
		}

		public void expectTransactionsForAccountUsingId() {
			Mockito.when(this.transferDao.getTransactionsForAccountUsingId(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt()))
					.thenReturn(this.pairs);
		}

		public void expectBlocksForAccount() {
			Mockito.when(this.blockDao.getBlocksForAccount(Mockito.any(), Mockito.any(), Mockito.anyInt())).thenReturn(this.blocks);
		}

		// endregion

		// region seedDefaultTransactions / addTransaction

		public void seedDefaultTransactions() {
			this.addTransaction(12, 7, 111, this.transactionHashes.get(0));
			this.addTransaction(12, 8, 222, this.transactionHashes.get(1));
			this.addTransaction(15, 9, 333, this.transactionHashes.get(2));
		}

		public void addTransaction(final int height, final int transactionId, final int amount, final Hash hash) {
			final DbBlock block = NisUtils.createDbBlockWithTimeStampAtHeight(123, height);
			final DbTransferTransaction dbTransferTransaction = this.createTransfer(amount);
			dbTransferTransaction.setId((long) transactionId);
			dbTransferTransaction.setTransferHash(hash);
			this.pairs.add(new TransferBlockPair(dbTransferTransaction, block));
		}

		private DbTransferTransaction createTransfer(final int amount) {
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, signer, recipient, Amount.fromNem(amount),
					null);
			transaction.sign();

			final DbTransferTransaction dbTransferTransaction = new DbTransferTransaction();
			Mockito.when(this.mapper.map(dbTransferTransaction)).thenReturn(transaction);
			return dbTransferTransaction;
		}

		// endregion

		// region assertDefaultTransactions

		public void assertDefaultTransactions(final SerializableList<TransactionMetaDataPair> pairs) {
			MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(3));
			Mockito.verify(this.mapper, Mockito.times(3)).map(Mockito.any(AbstractBlockTransfer.class));

			// heights
			final Collection<Long> heights = pairs.asCollection().stream().map(p -> p.getMetaData().getHeight().getRaw())
					.collect(Collectors.toList());
			MatcherAssert.assertThat(heights, IsEquivalent.equivalentTo(12L, 12L, 15L));

			// ids
			final Collection<Long> ids = pairs.asCollection().stream().map(p -> p.getMetaData().getId()).collect(Collectors.toList());
			MatcherAssert.assertThat(ids, IsEquivalent.equivalentTo(7L, 8L, 9L));

			// amounts
			final Collection<Long> amounts = pairs.asCollection().stream()
					.map(p -> ((TransferTransaction) p.getEntity()).getAmount().getNumNem()).collect(Collectors.toList());
			MatcherAssert.assertThat(amounts, IsEquivalent.equivalentTo(111L, 222L, 333L));

			// hashes
			final Collection<Hash> hashes = pairs.asCollection().stream().map(p -> p.getMetaData().getHash()).collect(Collectors.toList());
			MatcherAssert.assertThat(hashes, IsEquivalent.equivalentTo(this.transactionHashes));
		}

		// endregion

		// region seedDefaultBlocks

		public void seedDefaultBlocks() {
			final SecureRandom random = new SecureRandom();
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(897, 444));
			this.blocks.get(0).setTotalFee(8L);
			this.blocks.get(0).setId(random.nextLong());
			this.blocks.get(0).setDifficulty(random.nextLong());
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(123, 777));
			this.blocks.get(1).setTotalFee(9L);
			this.blocks.get(1).setId(random.nextLong());
			this.blocks.get(1).setDifficulty(random.nextLong());
			this.blocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(345, 222));
			this.blocks.get(2).setTotalFee(7L);
			this.blocks.get(2).setId(random.nextLong());
			this.blocks.get(2).setDifficulty(random.nextLong());
		}

		public void assertDefaultBlocks(final SerializableList<HarvestInfo> harvestInfos) {
			final Collection<Long> heights = harvestInfos.asCollection().stream().map(p -> p.getBlockHeight().getRaw())
					.collect(Collectors.toList());
			MatcherAssert.assertThat(heights, IsEquivalent.equivalentTo(444L, 777L, 222L));

			final Collection<Integer> timeStamps = harvestInfos.asCollection().stream().map(p -> p.getTimeStamp().getRawTime())
					.collect(Collectors.toList());
			MatcherAssert.assertThat(timeStamps, IsEquivalent.equivalentTo(897, 123, 345));

			final Collection<Long> fees = harvestInfos.asCollection().stream().map(p -> p.getTotalFee().getNumMicroNem())
					.collect(Collectors.toList());
			MatcherAssert.assertThat(fees, IsEquivalent.equivalentTo(8L, 9L, 7L));

			final Collection<Long> ids = harvestInfos.asCollection().stream().map(HarvestInfo::getId).collect(Collectors.toList());
			MatcherAssert.assertThat(ids, IsEquivalent.equivalentTo(this.blocks.stream().map(DbBlock::getId).collect(Collectors.toList())));

			final Collection<Long> difficulties = harvestInfos.asCollection().stream().map(HarvestInfo::getDifficulty)
					.collect(Collectors.toList());
			MatcherAssert.assertThat(difficulties,
					IsEquivalent.equivalentTo(this.blocks.stream().map(DbBlock::getDifficulty).collect(Collectors.toList())));
		}

		// endregion
	}
}
