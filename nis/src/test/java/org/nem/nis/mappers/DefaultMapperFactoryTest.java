package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
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

	//region registration

	private static class Entry<TDbModel, TModel> {
		public final Class<TDbModel> dbModelClass;
		public final Class<TModel> modelClass;

		private Entry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}
	}

	private static final List<Entry<?, ?>> OTHER_ENTRIES = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(DbAccount.class, Account.class));
			this.add(new Entry<>(DbBlock.class, Block.class));
			this.add(new Entry<>(DbMultisigSignatureTransaction.class, MultisigSignatureTransaction.class));
			this.add(new Entry<>(DbNamespace.class, Namespace.class));
			this.add(new Entry<>(DbMosaicDefinition.class, MosaicDefinition.class));
			this.add(new Entry<>(DbMosaicProperty.class, NemProperty.class));
		}
	};

	private static class TransactionEntry<TDbModel extends AbstractTransfer, TModel extends Transaction> extends Entry<TDbModel, TModel> {
		private TransactionEntry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			super(dbModelClass, modelClass);
		}
	}

	private static final List<TransactionEntry<?, ?>> TRANSACTION_ENTRIES = TransactionRegistry.stream()
			.map(e -> new TransactionEntry<>(e.dbModelClass, e.modelClass))
			.collect(Collectors.toList());

	@Test
	public void canCreateModelToDbModelMapper() {
		// Act:
		final DefaultMapperFactory factory = MapperUtils.createMapperFactory();
		final MappingRepository mapper = factory.createModelToDbModelMapper(Mockito.mock(AccountDaoLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(1 + OTHER_ENTRIES.size() + TRANSACTION_ENTRIES.size()));

		for (final Entry<?, ?> entry : OTHER_ENTRIES) {
			Assert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : TRANSACTION_ENTRIES) {
			Assert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}
	}

	@Test
	public void canCreateDbModelToModelMapper() {
		// Act:
		final DefaultMapperFactory factory = MapperUtils.createMapperFactory();
		final MappingRepository mapper = factory.createDbModelToModelMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(2 + OTHER_ENTRIES.size() + TRANSACTION_ENTRIES.size() * 2));

		for (final Entry<?, ?> entry : OTHER_ENTRIES) {
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : TRANSACTION_ENTRIES) {
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, Transaction.class), IsEqual.equalTo(true));
		}

		Assert.assertThat(mapper.isSupported(DbBlock.class, ExplorerBlockViewModel.class), IsEqual.equalTo(true));
	}

	//endregion

	//region integration

	@Test
	public void mapperSharesUnseenAddresses() {
		// Act:
		final DbBlock dbBlock = mapBlockWithMosaicTransactions();
		final DbMosaicDefinitionCreationTransaction dbMosaicDefinitionCreationTransaction = dbBlock.getBlockMosaicDefinitionCreationTransactions().get(0);
		final DbMosaicSupplyChangeTransaction dbSupplyChangeTransaction = dbBlock.getBlockMosaicSupplyChangeTransactions().get(0);

		// Assert:
		Assert.assertThat(
				dbMosaicDefinitionCreationTransaction.getSender(),
				IsSame.sameInstance(dbSupplyChangeTransaction.getSender()));
	}

	private static DbBlock mapBlockWithMosaicTransactions() {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		final Account blockSigner = Utils.generateRandomAccount();
		final Block block = new Block(blockSigner, Hash.ZERO, Hash.ZERO, new TimeInstant(123), new BlockHeight(111));

		final Account mosaicCreator = Utils.generateRandomAccount();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(mosaicCreator);
		final MosaicDefinitionCreationTransaction mosaicDefinitionCreationTransaction = new MosaicDefinitionCreationTransaction(
				TimeInstant.ZERO,
				mosaicCreator,
				mosaicDefinition);
		final MosaicSupplyChangeTransaction supplyChangeTransaction = new MosaicSupplyChangeTransaction(
				TimeInstant.ZERO,
				mosaicCreator,
				mosaicDefinition.getId(),
				MosaicSupplyType.Create,
				new Supply(1234));

		for (final Transaction t : Arrays.asList(mosaicDefinitionCreationTransaction, supplyChangeTransaction)) {
			t.sign();
			block.addTransaction(t);
		}

		block.sign();

		mockAccountDao.addMappings(block);
		mockAccountDao.addMappings(block);
		return toDbModel(block, accountDaoLookup);
	}

	private static DbBlock toDbModel(final Block block, final AccountDaoLookup accountDaoLookup) {
		return MapperUtils.toDbModelWithHack(block, accountDaoLookup);
	}

	//endregion
}