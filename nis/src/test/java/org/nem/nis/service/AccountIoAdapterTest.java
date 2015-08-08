package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
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
		final AccountIoAdapter accountIoAdapter = createAccountIoAdapter(accountCache);

		// Assert:
		for (int i = 0; i < 10; ++i) {
			Assert.assertThat(accountIoAdapter.findByAddress(accounts.get(i).getAddress()), IsEqual.equalTo(accounts.get(i)));
		}
	}

	private static AccountIoAdapter createAccountIoAdapter(final AccountCache accountCache) {
		return new AccountIoAdapter(
				null,
				null,
				null,
				null,
				accountCache,
				Mockito.mock(NisDbModelToModelMapper.class));
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

	//region namespaces

	@Test
	public void getAccountNamespacesReturnsEmptyListWhenAccountIsNotKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectNamespacesForAccount();
		context.seedDefaultNamespaces();
		context.expectUnknownAccount();

		// Act:
		final SerializableList<Namespace> namespaces = context.accountIoAdapter.getAccountNamespaces(context.address, new NamespaceId("foo"));

		// Assert:
		Assert.assertThat(namespaces.size(), IsEqual.equalTo(0));
		Mockito.verify(context.namespaceDao, Mockito.never()).getNamespacesForAccount(Mockito.any(), Mockito.any(), Mockito.anyInt());
	}

	@Test
	public void getAccountNamespacesDelegatesToNamespaceDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectNamespacesForAccount();
		context.seedDefaultNamespaces();

		// Act:
		final SerializableList<Namespace> namespaces = context.accountIoAdapter.getAccountNamespaces(context.address, new NamespaceId("foo"));

		// Assert:
		context.assertDefaultNamespaces(namespaces);
		Mockito.verify(context.namespaceDao, Mockito.only()).getNamespacesForAccount(context.account, new NamespaceId("foo"), DEFAULT_LIMIT);
	}

	//endregion

	//region mosaic definitions

	@Test
	public void getAccountMosaicDefinitionsReturnsEmptyListWhenAccountIsNotKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectMosaicsForAccount();
		context.seedDefaultMosaicDefinitions();
		context.expectUnknownAccount();

		// Act:
		final SerializableList<MosaicDefinition> mosaicDefinitions = context.accountIoAdapter.getAccountMosaicDefinitions(
				context.address,
				new NamespaceId("foo"),
				Long.MAX_VALUE);

		// Assert:
		Assert.assertThat(mosaicDefinitions.size(), IsEqual.equalTo(0));
		Mockito.verify(context.mosaicDefinitionDao, Mockito.never())
				.getMosaicDefinitionsForAccount(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyInt());
	}

	@Test
	public void getAccountMosaicDefinitionsDelegatesToMosaicDefinitionDao() {
		// Arrange:
		final TestContext context = new TestContext();
		context.expectMosaicsForAccount();
		context.seedDefaultMosaicDefinitions();

		// Act:
		final SerializableList<MosaicDefinition> mosaicDefinitions = context.accountIoAdapter.getAccountMosaicDefinitions(
				context.address,
				new NamespaceId("foo"),
				Long.MAX_VALUE);

		// Assert:
		context.assertDefaultMosaicDefinitions(mosaicDefinitions);
		Mockito.verify(context.mosaicDefinitionDao, Mockito.only())
				.getMosaicDefinitionsForAccount(context.account, new NamespaceId("foo"), Long.MAX_VALUE, DEFAULT_LIMIT);
	}

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final ReadOnlyTransferDao transferDao = Mockito.mock(ReadOnlyTransferDao.class);
		private final ReadOnlyNamespaceDao namespaceDao = Mockito.mock(ReadOnlyNamespaceDao.class);
		private final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao = Mockito.mock(ReadOnlyMosaicDefinitionDao.class);
		private final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		private final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(
				this.transferDao,
				this.blockDao,
				this.namespaceDao,
				this.mosaicDefinitionDao,
				this.accountCache,
				this.mapper);
		private final Account account = Utils.generateRandomAccount();
		private final Address address = this.account.getAddress();
		private final List<TransferBlockPair> pairs = new ArrayList<>();
		private final List<DbBlock> blocks = new ArrayList<>();
		private final List<DbNamespace> namespaces = new ArrayList<>();
		private final List<DbMosaicDefinition> mosaicDefinitions = new ArrayList<>();
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

		//region expect

		public void expectUnknownAccount() {
			Mockito.when(this.accountCache.findByAddress(this.address)).thenReturn(null);
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

		public void expectNamespacesForAccount() {
			Mockito.when(this.namespaceDao.getNamespacesForAccount(Mockito.any(), Mockito.any(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.namespaces);
		}

		public void expectMosaicsForAccount() {
			Mockito.when(this.mosaicDefinitionDao.getMosaicDefinitionsForAccount(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.eq(DEFAULT_LIMIT)))
					.thenReturn(this.mosaicDefinitions);
		}

		//endregion

		//region seedDefaultTransactions / addTransaction

		public void seedDefaultTransactions() {
			this.addTransaction(12, 7, 111, this.transactionHashes.get(0));
			this.addTransaction(12, 8, 222, this.transactionHashes.get(1));
			this.addTransaction(15, 9, 333, this.transactionHashes.get(2));
		}

		public void addTransaction(final int height, final int transactionId, final int amount, final Hash hash) {
			final DbBlock block = NisUtils.createDbBlockWithTimeStampAtHeight(123, height);
			final DbTransferTransaction dbTransferTransaction = this.createTransfer(amount);
			dbTransferTransaction.setId((long)transactionId);
			dbTransferTransaction.setTransferHash(hash);
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
			Mockito.verify(this.mapper, Mockito.times(3)).map(Mockito.any(AbstractBlockTransfer.class));

			// heights
			final Collection<Long> heights = pairs.asCollection().stream()
					.map(p -> p.getMetaData().getHeight().getRaw())
					.collect(Collectors.toList());
			Assert.assertThat(heights, IsEquivalent.equivalentTo(12L, 12L, 15L));

			// ids
			final Collection<Long> ids = pairs.asCollection().stream()
					.map(p -> p.getMetaData().getId())
					.collect(Collectors.toList());
			Assert.assertThat(ids, IsEquivalent.equivalentTo(7L, 8L, 9L));

			// amounts
			final Collection<Long> amounts = pairs.asCollection().stream()
					.map(p -> ((TransferTransaction)p.getEntity()).getAmount().getNumNem())
					.collect(Collectors.toList());
			Assert.assertThat(amounts, IsEquivalent.equivalentTo(111L, 222L, 333L));

			// hashes
			final Collection<Hash> hashes = pairs.asCollection().stream()
					.map(p -> p.getMetaData().getHash())
					.collect(Collectors.toList());
			Assert.assertThat(hashes, IsEquivalent.equivalentTo(this.transactionHashes));
		}

		//endregion

		//region seedDefaultBlocks

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

			final Collection<Long> ids = harvestInfos.asCollection().stream()
					.map(HarvestInfo::getId)
					.collect(Collectors.toList());
			Assert.assertThat(ids, IsEquivalent.equivalentTo(this.blocks.stream().map(DbBlock::getId).collect(Collectors.toList())));

			final Collection<Long> difficulties = harvestInfos.asCollection().stream()
					.map(HarvestInfo::getDifficulty)
					.collect(Collectors.toList());
			Assert.assertThat(difficulties, IsEquivalent.equivalentTo(this.blocks.stream().map(DbBlock::getDifficulty).collect(Collectors.toList())));
		}

		//endregion

		//region seedDefaultNamespaces

		public void seedDefaultNamespaces() {
			final String[] names = { "foo", "foo.bar", "baz" };
			final Long[] heights = { 222L, 444L, 666L };
			for (int i = 0; i < 3; i++) {
				final DbNamespace dbNamespace = new DbNamespace();
				dbNamespace.setFullName(names[i]);
				dbNamespace.setHeight(heights[i]);
				this.namespaces.add(dbNamespace);
				final Namespace namespace = new Namespace(new NamespaceId(names[i]), Utils.generateRandomAccount(), new BlockHeight(heights[i]));
				Mockito.when(this.mapper.map(dbNamespace)).thenReturn(namespace);
			}
		}

		public void assertDefaultNamespaces(final SerializableList<Namespace> namespaces) {
			final Collection<String> names = namespaces.asCollection().stream()
					.map(n -> n.getId().toString())
					.collect(Collectors.toList());
			Assert.assertThat(names, IsEquivalent.equivalentTo("foo", "foo.bar", "baz"));

			final Collection<Long> heights = namespaces.asCollection().stream()
					.map(n -> n.getHeight().getRaw())
					.collect(Collectors.toList());
			Assert.assertThat(heights, IsEquivalent.equivalentTo(222L, 444L, 666L));
		}

		//endregion

		//region seedDefaultMosaicDefinitions

		public void seedDefaultMosaicDefinitions() {
			final String[] ids = { "foo", "foo.bar", "baz" };
			final String[] names = { "food", "drinks", "trash" };
			for (int i = 0; i < 3; i++) {
				final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
				dbMosaicDefinition.setNamespaceId(ids[i]);
				dbMosaicDefinition.setName(names[i]);
				this.mosaicDefinitions.add(dbMosaicDefinition);
				final MosaicDefinition mosaicDefinition = new MosaicDefinition(
						Utils.generateRandomAccount(),
						new MosaicId(new NamespaceId(ids[i]), names[i]),
						new MosaicDescriptor("a mosaic"),
						Utils.createMosaicProperties(),
						null);
				Mockito.when(this.mapper.map(dbMosaicDefinition)).thenReturn(mosaicDefinition);
			}
		}

		public void assertDefaultMosaicDefinitions(final SerializableList<MosaicDefinition> mosaicDefinitions) {
			final Collection<String> ids = mosaicDefinitions.asCollection().stream()
					.map(m -> m.getId().getNamespaceId().toString())
					.collect(Collectors.toList());
			Assert.assertThat(ids, IsEquivalent.equivalentTo("foo", "foo.bar", "baz"));

			final Collection<String> names = mosaicDefinitions.asCollection().stream()
					.map(m -> m.getId().getName())
					.collect(Collectors.toList());
			Assert.assertThat(names, IsEquivalent.equivalentTo("food", "drinks", "trash"));
		}

		//endregion
	}

	// endregion
}
