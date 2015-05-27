package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MultisigAggregateModificationDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbMultisigAggregateModificationTransaction, MultisigAggregateModificationTransaction> {

	@Test
	public void transferWithNoModificationsCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(dbModel),
				IllegalArgumentException.class);
	}

	@Test
	public void transferWithSingleModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 1);
	}

	@Test
	public void transferWithMultipleModificationsCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		context.addModification(2);
		context.addModification(1);
		final DbMultisigAggregateModificationTransaction dbModel = context.createDbModel();

		// Act:
		final MultisigAggregateModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 3);
	}

	@Override
	protected DbMultisigAggregateModificationTransaction createDbModel() {
		final DbMultisigAggregateModificationTransaction transfer = new DbMultisigAggregateModificationTransaction();
		final Set<DbMultisigModification> modifications = new HashSet<>();
		modifications.add(createModification(new DbAccount(1), 1));
		transfer.setMultisigModifications(modifications);
		return transfer;
	}

	@Override
	protected IMapping<DbMultisigAggregateModificationTransaction, MultisigAggregateModificationTransaction> createMapping(final IMapper mapper) {
		return new MultisigAggregateModificationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Map<org.nem.core.model.Account, Integer> expectedModifications = new HashMap<>();
		private final Set<DbMultisigModification> modifications = new HashSet<>();
		private final MultisigAggregateModificationDbModelToModelMapping mapping = new MultisigAggregateModificationDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, org.nem.core.model.Account.class)).thenReturn(this.sender);
		}

		private void addModification(final int type) {
			final DbAccount dbCosignatory = Mockito.mock(DbAccount.class);
			final org.nem.core.model.Account cosignatory = Utils.generateRandomAccount();
			Mockito.when(this.mapper.map(dbCosignatory, org.nem.core.model.Account.class)).thenReturn(cosignatory);

			this.modifications.add(createModification(dbCosignatory, type));
			this.expectedModifications.put(cosignatory, type);
		}

		public DbMultisigAggregateModificationTransaction createDbModel() {
			final DbMultisigAggregateModificationTransaction dbModification = new DbMultisigAggregateModificationTransaction();
			dbModification.setTimeStamp(4444);
			dbModification.setSender(this.dbSender);

			dbModification.setMultisigModifications(this.modifications);

			// zero out required fields
			dbModification.setFee(0L);
			dbModification.setDeadline(0);
			return dbModification;
		}

		public void assertModel(final MultisigAggregateModificationTransaction model, final int numExpectedModifications) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));

			Assert.assertThat(model.getCosignatoryModifications().size(), IsEqual.equalTo(numExpectedModifications));
			final Map<org.nem.core.model.Account, Integer> actualModifications = new HashMap<>();
			for (final MultisigCosignatoryModification modification : model.getCosignatoryModifications()) {
				actualModifications.put(modification.getCosignatory(), modification.getModificationType().value());
			}

			Assert.assertThat(actualModifications, IsEqual.equalTo(this.expectedModifications));
		}
	}

	private static DbMultisigModification createModification(final DbAccount cosignatory, final int type) {
		final DbMultisigModification dbModification = new DbMultisigModification();
		dbModification.setCosignatory(cosignatory);
		dbModification.setModificationType(type);
		return dbModification;
	}
}