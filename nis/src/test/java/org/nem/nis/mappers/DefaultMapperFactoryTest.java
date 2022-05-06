package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultMapperFactoryTest {

	@Before
	public void setup() {
		Utils.setupGlobals();
	}

	@After
	public void destroy() {
		Utils.resetGlobals();
	}

	// region registration

	private static class Entry<TDbModel, TModel> {
		public final Class<TDbModel> dbModelClass;
		public final Class<TModel> modelClass;

		private Entry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}
	}

	@SuppressWarnings("serial")
	private static final List<Entry<?, ?>> OTHER_ENTRIES = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(DbAccount.class, Account.class));
			this.add(new Entry<>(DbBlock.class, Block.class));
			this.add(new Entry<>(DbMultisigSignatureTransaction.class, MultisigSignatureTransaction.class));
			this.add(new Entry<>(DbNamespace.class, Namespace.class));
			this.add(new Entry<>(DbMosaicDefinition.class, MosaicDefinition.class));
			this.add(new Entry<>(DbMosaicProperty.class, NemProperty.class));
			this.add(new Entry<>(DbMosaic.class, Mosaic.class));
		}
	};

	private static class TransactionEntry<TDbModel extends AbstractTransfer, TModel extends Transaction> extends Entry<TDbModel, TModel> {
		private TransactionEntry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			super(dbModelClass, modelClass);
		}
	}

	@SuppressWarnings("rawtypes")
	private static final List<TransactionEntry<?, ?>> TRANSACTION_ENTRIES = TransactionRegistry.stream()
			.map(e -> new TransactionEntry<>(e.dbModelClass, e.modelClass)).collect(Collectors.toList());

	@Test
	public void canCreateModelToDbModelMapper() {
		// Act:
		final DefaultMapperFactory factory = MapperUtils.createMapperFactory();
		final MappingRepository mapper = factory.createModelToDbModelMapper(Mockito.mock(AccountDaoLookup.class));

		// Assert:
		MatcherAssert.assertThat(mapper, IsNull.notNullValue());
		MatcherAssert.assertThat(mapper.size(), IsEqual.equalTo(1 + OTHER_ENTRIES.size() + TRANSACTION_ENTRIES.size()));

		for (final Entry<?, ?> entry : OTHER_ENTRIES) {
			MatcherAssert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : TRANSACTION_ENTRIES) {
			MatcherAssert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}
	}

	@Test
	public void canCreateDbModelToModelMapper() {
		// Act:
		final DefaultMapperFactory factory = MapperUtils.createMapperFactory();
		final MappingRepository mapper = factory.createDbModelToModelMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		MatcherAssert.assertThat(mapper, IsNull.notNullValue());
		MatcherAssert.assertThat(mapper.size(), IsEqual.equalTo(2 + OTHER_ENTRIES.size() + TRANSACTION_ENTRIES.size() * 2));

		for (final Entry<?, ?> entry : OTHER_ENTRIES) {
			MatcherAssert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : TRANSACTION_ENTRIES) {
			MatcherAssert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
			MatcherAssert.assertThat(mapper.isSupported(entry.dbModelClass, Transaction.class), IsEqual.equalTo(true));
		}

		MatcherAssert.assertThat(mapper.isSupported(DbBlock.class, ExplorerBlockViewModel.class), IsEqual.equalTo(true));
	}

	// endregion

	// region integration - mosaic ids

	@Test
	public void canMapHistoricallyDifferentButEquivalentMosaicIdsToLatestDbMosaicId() {
		// Act:
		final DbBlock dbBlock = mapBlockWithMosaicTransferTransactions();
		final DbTransferTransaction dbTransfer1 = dbBlock.getBlockTransferTransactions().get(0);
		final DbTransferTransaction dbTransfer2 = dbBlock.getBlockTransferTransactions().get(1);

		// Assert:
		// (this test is ok because the model -> dbmodel mapping only happens before a dbmodel is saved,
		// which requires the latest db mosaic ids)
		MatcherAssert.assertThat(getFirst(dbTransfer1.getMosaics()).getDbMosaicId(), IsEqual.equalTo(20L));
		MatcherAssert.assertThat(getFirst(dbTransfer2.getMosaics()).getDbMosaicId(), IsEqual.equalTo(20L));
	}

	@Test
	public void canMapDifferentDbMosaicIdsToSameMosaicId() {
		// Act:
		final AccountLookup accountLookup = new DefaultAccountCache();
		final MosaicIdCache mosaicIdCache = new DefaultMosaicIdCache();
		final DbBlock dbBlock = createDbBlockWithDbMosaicTransfers(accountLookup, mosaicIdCache);
		final DbTransferTransaction dbTransfer1 = dbBlock.getBlockTransferTransactions().get(0);
		final DbTransferTransaction dbTransfer2 = dbBlock.getBlockTransferTransactions().get(1);

		// sanity check
		MatcherAssert.assertThat(getFirst(dbTransfer1.getMosaics()).getDbMosaicId(), IsEqual.equalTo(10L));
		MatcherAssert.assertThat(getFirst(dbTransfer2.getMosaics()).getDbMosaicId(), IsEqual.equalTo(20L));

		// Act:
		final Block block = MapperUtils.toModel(dbBlock, accountLookup, mosaicIdCache);
		final TransferTransaction transaction1 = (TransferTransaction) block.getTransactions().get(0);
		final TransferTransaction transaction2 = (TransferTransaction) block.getTransactions().get(1);

		// Assert:
		MatcherAssert.assertThat(getFirst(transaction1.getAttachment().getMosaics()).getMosaicId(),
				IsEqual.equalTo(Utils.createMosaicId(5)));
		MatcherAssert.assertThat(getFirst(transaction2.getAttachment().getMosaics()).getMosaicId(),
				IsEqual.equalTo(Utils.createMosaicId(5)));
	}

	private static DbBlock mapBlockWithMosaicTransferTransactions() {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		final TransferTransaction transfer1 = createMosaicTransfer(Utils.generateRandomAccount());
		final TransferTransaction transfer2 = createMosaicTransfer(Utils.generateRandomAccount());

		final Block block = createBlock(Utils.generateRandomAccount(), Arrays.asList(transfer1, transfer2));

		mockAccountDao.addMappings(block);

		final MosaicIdCache mosaicIdCache = new DefaultMosaicIdCache();
		mosaicIdCache.add(getFirst(transfer1.getAttachment().getMosaics()).getMosaicId(), new DbMosaicId(10L));
		mosaicIdCache.add(getFirst(transfer2.getAttachment().getMosaics()).getMosaicId(), new DbMosaicId(20L));
		return MapperUtils.toDbModel(block, accountDaoLookup, mosaicIdCache);
	}

	private static DbBlock createDbBlockWithDbMosaicTransfers(final AccountLookup accountLookup, final MosaicIdCache mosaicIdCache) {
		final Address harvester = Utils.generateRandomAddressWithPublicKey();
		final Address signerAndRecipient = Utils.generateRandomAddressWithPublicKey();
		accountLookup.findByAddress(harvester);
		accountLookup.findByAddress(signerAndRecipient);

		final MosaicId mosaicId = Utils.createMosaicId(5);
		mosaicIdCache.add(mosaicId, new DbMosaicId(10L));
		mosaicIdCache.add(mosaicId, new DbMosaicId(20L));

		final DbBlock dbBlock = NisUtils.createDummyDbBlock(new DbAccount(harvester));
		dbBlock.addTransferTransaction(createDbMosaicTransfer(signerAndRecipient, 10L, 0));
		dbBlock.addTransferTransaction(createDbMosaicTransfer(signerAndRecipient, 20L, 1));
		return dbBlock;
	}

	private static <T> T getFirst(final Collection<T> items) {
		return items.iterator().next();
	}

	private static TransferTransaction createMosaicTransfer(final Account signer) {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(signer);
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicDefinition.getId(), new Quantity(100));
		return new TransferTransaction(TimeInstant.ZERO, signer, Utils.generateRandomAccount(), Amount.fromNem(123), attachment);
	}

	private static DbTransferTransaction createDbMosaicTransfer(final Address signerAndRecipient, final Long id, final int blkIndex) {
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setDbMosaicId(id);
		dbMosaic.setQuantity(123L);
		final DbTransferTransaction transfer = createDbTransfer(signerAndRecipient, blkIndex);
		transfer.setVersion(2);
		transfer.setMosaics(Collections.singleton(dbMosaic));
		return transfer;
	}

	private static DbTransferTransaction createDbTransfer(final Address signerAndRecipient, final int blkIndex) {
		final DbTransferTransaction dbTransfer = RandomDbTransactionFactory.createTransfer();
		dbTransfer.setSender(new DbAccount(signerAndRecipient));
		dbTransfer.setRecipient(new DbAccount(signerAndRecipient));
		dbTransfer.setBlkIndex(blkIndex);
		return dbTransfer;
	}

	// endregion

	// region integration - addresses

	@Test
	public void mapperSharesUnseenAddresses() {
		// Act:
		final DbBlock dbBlock = mapBlockWithMosaicTransactions();
		final DbMosaicDefinitionCreationTransaction dbMosaicDefinitionCreationTransaction = dbBlock
				.getBlockMosaicDefinitionCreationTransactions().get(0);
		final DbMosaicSupplyChangeTransaction dbSupplyChangeTransaction = dbBlock.getBlockMosaicSupplyChangeTransactions().get(0);

		// Assert:
		MatcherAssert.assertThat(dbMosaicDefinitionCreationTransaction.getSender(),
				IsSame.sameInstance(dbSupplyChangeTransaction.getSender()));
	}

	private static DbBlock mapBlockWithMosaicTransactions() {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		final Account mosaicCreator = Utils.generateRandomAccount();
		final Account creationFeeSink = Utils.generateRandomAccount();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(mosaicCreator);
		final MosaicDefinitionCreationTransaction mosaicDefinitionCreationTransaction = new MosaicDefinitionCreationTransaction(
				TimeInstant.ZERO, mosaicCreator, mosaicDefinition, creationFeeSink, Amount.fromNem(25));
		final MosaicSupplyChangeTransaction supplyChangeTransaction = new MosaicSupplyChangeTransaction(TimeInstant.ZERO, mosaicCreator,
				mosaicDefinition.getId(), MosaicSupplyType.Create, new Supply(1234));

		final Block block = createBlock(Utils.generateRandomAccount(),
				Arrays.asList(mosaicDefinitionCreationTransaction, supplyChangeTransaction));

		mockAccountDao.addMappings(block);
		return toDbModel(block, accountDaoLookup);
	}

	// endregion

	private static Block createBlock(final Account signer) {
		return new Block(signer, Hash.ZERO, Hash.ZERO, new TimeInstant(123), new BlockHeight(111));
	}

	private static Block createBlock(final Account blockSigner, final Collection<Transaction> transactions) {
		final Block block = createBlock(blockSigner);

		for (final Transaction t : transactions) {
			t.sign();
			block.addTransaction(t);
		}

		block.sign();
		return block;
	}

	private static DbBlock toDbModel(final Block block, final AccountDaoLookup accountDaoLookup) {
		return MapperUtils.toDbModelWithHack(block, accountDaoLookup);
	}
}
