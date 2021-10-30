package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ProvisionNamespaceModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = context.createModel();

		// Act:
		final DbProvisionNamespaceTransaction dbModel = context.mapping.map(transaction);

		// Assert:
		// - the transaction properties
		MatcherAssert.assertThat(dbModel.getRentalFeeSink(), IsEqual.equalTo(context.dbRentalFeeSink));
		MatcherAssert.assertThat(dbModel.getRentalFee(), IsEqual.equalTo(25_000_000L));
		MatcherAssert.assertThat(dbModel.getNamespace(), IsEqual.equalTo(context.dbNamespace));
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

		// - the namespace properties (passed to the sub-mapper)
		final ArgumentCaptor<Namespace> namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(namespaceCaptor.capture(), Mockito.eq(DbNamespace.class));
		final Namespace namespace = namespaceCaptor.getValue();
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar.baz")));
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.sender));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	@Test
	public void transactionNamespaceHeightIsNotMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.dbNamespace.setHeight(111L);
		final ProvisionNamespaceTransaction transaction = context.createModel();

		// Act:
		final DbProvisionNamespaceTransaction dbModel = context.mapping.map(transaction);

		// Assert:
		MatcherAssert.assertThat(dbModel.getNamespace().getHeight(), IsNull.nullValue());
	}

	@Test
	public void transactionNamespaceCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = context.createModel();

		// Act:
		context.mapping.map(transaction);

		// Assert:
		// - the namespace properties (passed to the sub-mapper)
		final ArgumentCaptor<Namespace> namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(namespaceCaptor.capture(), Mockito.eq(DbNamespace.class));
		final Namespace namespace = namespaceCaptor.getValue();
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar.baz")));
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.sender));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	@Override
	protected ProvisionNamespaceTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new ProvisionNamespaceTransaction(timeStamp, sender, Utils.generateRandomAccount(), Amount.fromNem(25000),
				new NamespaceIdPart("baz"), new NamespaceId("foo.bar"));
	}

	@Override
	protected IMapping<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbNamespace.class))).thenReturn(new DbNamespace());
		return new ProvisionNamespaceModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account rentalFeeSink = Utils.generateRandomAccount();
		private final DbAccount dbRentalFeeSink = Mockito.mock(DbAccount.class);
		private final Namespace namespace = new Namespace(new NamespaceId("foo.bar.baz"), this.sender, BlockHeight.MAX);
		private final DbNamespace dbNamespace = new DbNamespace();
		private final ProvisionNamespaceModelToDbModelMapping mapping = new ProvisionNamespaceModelToDbModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.namespace, DbNamespace.class)).thenReturn(this.dbNamespace);
			Mockito.when(this.mapper.map(this.rentalFeeSink, DbAccount.class)).thenReturn(this.dbRentalFeeSink);
		}

		private ProvisionNamespaceTransaction createModel() {
			return new ProvisionNamespaceTransaction(TimeInstant.ZERO, this.sender, this.rentalFeeSink, Amount.fromNem(25),
					this.namespace.getId().getLastPart(), this.namespace.getId().getParent());
		}
	}
}
