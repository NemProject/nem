package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ProvisionNamespaceModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceTransaction transaction = context.createModel();

		// Act:
		final DbProvisionNamespaceTransaction dbModel = context.mapping.map(transaction);

		// Assert:
		// - the transaction properties
		Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(context.dbLessor));
		Assert.assertThat(dbModel.getRentalFee(), IsEqual.equalTo(25_000_000L));
		Assert.assertThat(dbModel.getNamespace(), IsEqual.equalTo(context.dbNamespace));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

		// - the namespace properties (passed to the sub-mapper)
		final ArgumentCaptor<Namespace> namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(namespaceCaptor.capture(), Mockito.eq(DbNamespace.class));
		final Namespace namespace = namespaceCaptor.getValue();
		Assert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar.baz")));
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.sender));
		Assert.assertThat(namespace.getExpiryHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	@Override
	protected ProvisionNamespaceTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new ProvisionNamespaceTransaction(
				timeStamp,
				sender,
				Utils.generateRandomAccount(),
				Amount.fromNem(25000),
				new NamespaceIdPart("baz"),
				new NamespaceId("foo.bar"));
	}

	@Override
	protected IMapping<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> createMapping(final IMapper mapper) {
		return new ProvisionNamespaceModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account lessor = Utils.generateRandomAccount();
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private Namespace namespace = new Namespace(new NamespaceId("foo.bar.baz"), sender, BlockHeight.MAX);
		private DbNamespace dbNamespace = Mockito.mock(DbNamespace.class);
		private final ProvisionNamespaceModelToDbModelMapping mapping = new ProvisionNamespaceModelToDbModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.namespace, DbNamespace.class)).thenReturn(this.dbNamespace);
			Mockito.when(this.mapper.map(this.lessor, DbAccount.class)).thenReturn(this.dbLessor);
		}

		private ProvisionNamespaceTransaction createModel() {
			return new ProvisionNamespaceTransaction(
					TimeInstant.ZERO,
					this.sender,
					lessor,
					Amount.fromNem(25),
					this.namespace.getId().getLastPart(),
					this.namespace.getId().getParent());
		}
	}
}
