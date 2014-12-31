package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class DefaultMapperFactoryTest {

	private static class Entry<TDbModel extends AbstractTransfer, TModel extends Transaction> {
		public final Class<TDbModel> dbModelClass;
		public final Class<TModel> modelClass;

		private Entry(
				final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass) {
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}
	}

	private static final List<Entry<?, ?>> entries = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(
					Transfer.class,
					TransferTransaction.class));
			this.add(new Entry<>(
					ImportanceTransfer.class,
					ImportanceTransferTransaction.class));
		}
	};

	@Test
	public void canCreateModelToDbModelMapper() {
		// Act:
		final DefaultMapperFactory factory = new DefaultMapperFactory();
		final MappingRepository mapper = factory.createModelToDbModelMapper(Mockito.mock(AccountDaoLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(2 + entries.size()));
		Assert.assertThat(mapper.isSupported(Account.class, org.nem.nis.dbmodel.Account.class), IsEqual.equalTo(true));
		Assert.assertThat(mapper.isSupported(Block.class, org.nem.nis.dbmodel.Block.class), IsEqual.equalTo(true));
		for (final Entry<?, ?> entry : entries) {
			Assert.assertThat(mapper.isSupported(entry.modelClass, entry.dbModelClass), IsEqual.equalTo(true));
		}
	}

	@Test
	public void canCreateDbModelToModelMapper() {
		// Act:
		final DefaultMapperFactory factory = new DefaultMapperFactory();
		final MappingRepository mapper = factory.createDbModelToModelMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
		Assert.assertThat(mapper.size(), IsEqual.equalTo(2 + entries.size() * 2));
		Assert.assertThat(mapper.isSupported(org.nem.nis.dbmodel.Account.class, Account.class), IsEqual.equalTo(true));
		Assert.assertThat(mapper.isSupported(org.nem.nis.dbmodel.Block.class, Block.class), IsEqual.equalTo(true));
		for (final Entry<?, ?> entry : entries) {
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, entry.modelClass), IsEqual.equalTo(true));
			Assert.assertThat(mapper.isSupported(entry.dbModelClass, Transaction.class), IsEqual.equalTo(true));
		}
	}
}