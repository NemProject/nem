package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class DefaultMapperFactoryTest {

	private static class Entry<TDbModel, TModel> {
		public final Class<TDbModel> dbModelClass;
		public final Class<TModel> modelClass;

		private Entry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}
	}

	private static final List<Entry<?, ?>> otherEntries = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(org.nem.nis.dbmodel.Account.class, Account.class));
			this.add(new Entry<>(org.nem.nis.dbmodel.Block.class, Block.class));
			this.add(new Entry<>(DbMultisigSignatureTransaction.class, MultisigSignatureTransaction.class));
		}
	};

	private static class TransactionEntry<TDbModel extends AbstractTransfer, TModel extends Transaction> extends Entry<TDbModel, TModel> {
		private TransactionEntry(final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			super(dbModelClass, modelClass);
		}
	}

	private static final List<TransactionEntry<?, ?>> transactionEntries = new ArrayList<TransactionEntry<?, ?>>() {
		{
			this.add(new TransactionEntry<>(Transfer.class, TransferTransaction.class));
			this.add(new TransactionEntry<>(ImportanceTransfer.class, ImportanceTransferTransaction.class));
			this.add(new TransactionEntry<>(MultisigSignerModification.class, MultisigSignerModificationTransaction.class));
			this.add(new TransactionEntry<>(org.nem.nis.dbmodel.MultisigTransaction.class, MultisigTransaction.class));
		}
	};

	@Test
	public void canCreateModelToDbModelMapper() {
		// Act:
		final DefaultMapperFactory factory = new DefaultMapperFactory();
		final MappingRepository mapper = factory.createModelToDbModelMapper(Mockito.mock(AccountDaoLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(1 + otherEntries.size() + transactionEntries.size()));

		for (final Entry<?, ?> entry : otherEntries) {
			Assert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : transactionEntries) {
			Assert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}

		Assert.assertThat(mapper.isSupported(NemesisBlock.class, org.nem.nis.dbmodel.Block.class), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateDbModelToModelMapper() {
		// Act:
		final DefaultMapperFactory factory = new DefaultMapperFactory();
		final MappingRepository mapper = factory.createDbModelToModelMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(otherEntries.size() + transactionEntries.size() * 2));

		for (final Entry<?, ?> entry : otherEntries) {
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
		}

		for (final TransactionEntry<?, ?> entry : transactionEntries) {
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, Transaction.class), IsEqual.equalTo(true));
		}
	}
}