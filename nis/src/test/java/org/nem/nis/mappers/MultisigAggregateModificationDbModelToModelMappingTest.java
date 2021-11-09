package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MultisigAggregateModificationDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbMultisigAggregateModificationTransaction, MultisigAggregateModificationTransaction> {

	@Test
	public void transferWithNoCosignatoryModificationsAndNoMinCosignatoriesModificationCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		ExceptionAssert.assertThrows(v -> context.mapping.map(dbModel), IllegalArgumentException.class);
	}

	@Test
	public void transferWithNoSingleCosignatoryModificationAndWithMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();
		dbModel.setMultisigMinCosignatoriesModification(createMinCosignatoriesModification(1));

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 0, 1);
	}

	@Test
	public void transferWithSingleCosignatoryModificationAndNoMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 1, 0);
	}

	@Test
	public void transferWithSingleCosignatoryModificationAndWithMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();
		dbModel.setMultisigMinCosignatoriesModification(createMinCosignatoriesModification(1));

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 1, 1);
	}

	@Test
	public void transferWithMultipleCosignatoryModificationsAndNoMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryModification(1);
		context.addCosignatoryModification(2);
		context.addCosignatoryModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 3, 0);
	}

	@Test
	public void transferWithMultipleCosignatoryModificationsAndWithMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryModification(1);
		context.addCosignatoryModification(2);
		context.addCosignatoryModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();
		dbModel.setMultisigMinCosignatoriesModification(createMinCosignatoriesModification(2));

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 3, 2);
	}

	@Override
	protected DbMultisigAggregateModificationTransaction createDbModel() {
		final DbMultisigAggregateModificationTransaction transfer = new DbMultisigAggregateModificationTransaction();
		final Set<DbMultisigModification> modifications = new HashSet<>();
		modifications.add(createCosignatoryModification(new DbAccount(1), 1));
		transfer.setMultisigModifications(modifications);
		return transfer;
	}

	@Override
	protected IMapping<DbMultisigAggregateModificationTransaction, MultisigAggregateModificationTransaction> createMapping(
			final IMapper mapper) {
		return new MultisigAggregateModificationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Map<org.nem.core.model.Account, Integer> expectedModifications = new HashMap<>();
		private final Set<DbMultisigModification> modifications = new HashSet<>();
		private final MultisigAggregateModificationDbModelToModelMapping mapping = new MultisigAggregateModificationDbModelToModelMapping(
				this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, org.nem.core.model.Account.class)).thenReturn(this.sender);
		}

		private void addCosignatoryModification(final int type) {
			final DbAccount dbCosignatory = Mockito.mock(DbAccount.class);
			final org.nem.core.model.Account cosignatory = Utils.generateRandomAccount();
			Mockito.when(this.mapper.map(dbCosignatory, org.nem.core.model.Account.class)).thenReturn(cosignatory);

			this.modifications.add(createCosignatoryModification(dbCosignatory, type));
			this.expectedModifications.put(cosignatory, type);
		}

		public DbMultisigAggregateModificationTransaction createDbModel() {
			final DbMultisigAggregateModificationTransaction dbModification = new DbMultisigAggregateModificationTransaction();
			dbModification.setVersion(2);
			dbModification.setTimeStamp(4444);
			dbModification.setSender(this.dbSender);

			dbModification.setMultisigModifications(this.modifications);

			// zero out required fields
			dbModification.setFee(0L);
			dbModification.setDeadline(0);
			return dbModification;
		}

		public void assertModel(final MultisigAggregateModificationTransaction model, final int numExpectedModifications,
				final int expectedRelativeChange) {
			MatcherAssert.assertThat(model.getCosignatoryModifications().size(), IsEqual.equalTo(numExpectedModifications));
			final Map<org.nem.core.model.Account, Integer> actualModifications = new HashMap<>();
			for (final MultisigCosignatoryModification modification : model.getCosignatoryModifications()) {
				actualModifications.put(modification.getCosignatory(), modification.getModificationType().value());
			}

			MatcherAssert.assertThat(actualModifications, IsEqual.equalTo(this.expectedModifications));
			if (0 != expectedRelativeChange) {
				MatcherAssert.assertThat(model.getMinCosignatoriesModification().getRelativeChange(),
						IsEqual.equalTo(expectedRelativeChange));
			} else {
				MatcherAssert.assertThat(model.getMinCosignatoriesModification(), IsNull.nullValue());
			}
		}
	}

	private static DbMultisigModification createCosignatoryModification(final DbAccount cosignatory, final int type) {
		final DbMultisigModification dbModification = new DbMultisigModification();
		dbModification.setCosignatory(cosignatory);
		dbModification.setModificationType(type);
		return dbModification;
	}

	private static DbMultisigMinCosignatoriesModification createMinCosignatoriesModification(final int relativeChange) {
		final DbMultisigMinCosignatoriesModification dbMultisigMinCosignatoriesModification = new DbMultisigMinCosignatoriesModification();
		dbMultisigMinCosignatoriesModification.setRelativeChange(relativeChange);
		return dbMultisigMinCosignatoriesModification;
	}
}
