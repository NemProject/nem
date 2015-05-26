package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MultisigAggregateModificationModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> {

	@Test
	public void transferWithSingleModificationCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 1);
	}

	@Test
	public void transferWithMultipleModificationsCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		context.addModification(2);
		context.addModification(1);
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 3);
	}

	@Override
	protected MultisigAggregateModificationTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new MultisigAggregateModificationTransaction(
				timeStamp,
				sender,
				Arrays.asList(new MultisigModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())));
	}

	@Override
	protected IMapping<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> createMapping(final IMapper mapper) {
		return new MultisigAggregateModificationModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Map<DbAccount, Integer> expectedModifications = new HashMap<>();
		final List<org.nem.core.model.MultisigModification> modifications = new ArrayList<>();
		private final MultisigAggregateModificationModelToDbModelMapping mapping = new MultisigAggregateModificationModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.sender, DbAccount.class)).thenReturn(this.dbSender);
		}

		private void addModification(final int type) {
			final DbAccount dbCosignatory = Mockito.mock(DbAccount.class);
			final org.nem.core.model.Account cosignatory = Utils.generateRandomAccount();
			Mockito.when(this.mapper.map(cosignatory, DbAccount.class)).thenReturn(dbCosignatory);

			this.modifications.add(createModification(cosignatory, type));
			this.expectedModifications.put(dbCosignatory, type);
		}

		public MultisigAggregateModificationTransaction createModel() {
			return new MultisigAggregateModificationTransaction(
					TimeInstant.ZERO,
					this.sender,
					this.modifications);
		}

		public void assertModel(final DbMultisigAggregateModificationTransaction dbModel, final int numExpectedModifications) {
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			Assert.assertThat(dbModel.getMultisigModifications().size(), IsEqual.equalTo(numExpectedModifications));
			final Map<DbAccount, Integer> actualModifications = new HashMap<>();
			for (final DbMultisigModification modification : dbModel.getMultisigModifications()) {
				actualModifications.put(modification.getCosignatory(), modification.getModificationType());
			}

			Assert.assertThat(actualModifications, IsEqual.equalTo(this.expectedModifications));

			for (final DbMultisigModification modification : dbModel.getMultisigModifications()) {
				Assert.assertThat(modification.getMultisigAggregateModificationTransaction(), IsEqual.equalTo(dbModel));
			}
		}
	}

	private static org.nem.core.model.MultisigModification createModification(final Account cosignatory, final int type) {
		return new MultisigModification(MultisigModificationType.fromValueOrDefault(type), cosignatory);
	}
}