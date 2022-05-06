package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class ProvisionNamespaceDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbProvisionNamespaceTransaction, ProvisionNamespaceTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setNamespace(context.dbNamespace);
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setDeadline(4321);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setFee(123L);
		dbTransaction.setRentalFeeSink(context.dbRentalFeeSink);
		dbTransaction.setRentalFee(25_000_000L);

		// Act:
		final ProvisionNamespaceTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		MatcherAssert.assertThat(model.getRentalFeeSink(), IsEqual.equalTo(context.rentalFeeSink));
		MatcherAssert.assertThat(model.getRentalFee(), IsEqual.equalTo(Amount.fromNem(25)));
		MatcherAssert.assertThat(model.getNewPart(), IsEqual.equalTo(new NamespaceIdPart("baz")));
		MatcherAssert.assertThat(model.getParent(), IsEqual.equalTo(new NamespaceId("foo.bar")));
	}

	@Override
	protected DbProvisionNamespaceTransaction createDbModel() {
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setRentalFeeSink(new DbAccount());
		dbTransaction.setRentalFee(1L);
		dbTransaction.setNamespace(new DbNamespace());
		return dbTransaction;
	}

	@Override
	protected IMapping<DbProvisionNamespaceTransaction, ProvisionNamespaceTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Namespace.class)))
				.thenReturn(new Namespace(new NamespaceId("foo.bar.baz"), Utils.generateRandomAccount(), BlockHeight.MAX));
		return new ProvisionNamespaceDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Account rentalFeeSink = Utils.generateRandomAccount();
		private final DbAccount dbRentalFeeSink = Mockito.mock(DbAccount.class);
		private final Namespace namespace = new Namespace(new NamespaceId("foo.bar.baz"), this.sender, BlockHeight.MAX);
		private final DbNamespace dbNamespace = Mockito.mock(DbNamespace.class);
		private final ProvisionNamespaceDbModelToModelMapping mapping = new ProvisionNamespaceDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbNamespace, Namespace.class)).thenReturn(this.namespace);
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRentalFeeSink, Account.class)).thenReturn(this.rentalFeeSink);
		}
	}
}
