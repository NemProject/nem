package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class ProvisionNamespaceDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbProvisionNamespaceTransaction, ProvisionNamespaceTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setDeadline(4321);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setFee(123L);
		dbTransaction.setLessor(context.dbLessor);
		dbTransaction.setRentalFee(25_000_000L);
		dbTransaction.setNamespace(context.dbNamespace);

		// Act:
		final ProvisionNamespaceTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		Assert.assertThat(model.getLessor(), IsEqual.equalTo(context.lessor));
		Assert.assertThat(model.getRentalFee(), IsEqual.equalTo(Amount.fromNem(25)));
		Assert.assertThat(model.getNewPart(), IsEqual.equalTo(new NamespaceIdPart("baz")));
		Assert.assertThat(model.getParent(), IsEqual.equalTo(new NamespaceId("foo.bar")));
	}

	@Override
	protected DbProvisionNamespaceTransaction createDbModel() {
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setLessor(new DbAccount());
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
		private final Account lessor = Utils.generateRandomAccount();
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private final Namespace namespace = new Namespace(new NamespaceId("foo.bar.baz"), this.sender, BlockHeight.MAX);
		private final DbNamespace dbNamespace = Mockito.mock(DbNamespace.class);
		private final ProvisionNamespaceDbModelToModelMapping mapping = new ProvisionNamespaceDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbNamespace, Namespace.class)).thenReturn(this.namespace);
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbLessor, Account.class)).thenReturn(this.lessor);
		}
	}
}
