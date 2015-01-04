package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.MultisigModification;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.Account;

import java.util.*;
import java.util.stream.Collectors;

public class MultisigSignerModificationDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<MultisigSignerModification, MultisigSignerModificationTransaction> {

	@Test
	public void transferWithNoModificationsCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignerModification dbModel = context.createDbModel();

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
		final MultisigSignerModification dbModel = context.createDbModel();

		// Act:
		final MultisigSignerModificationTransaction model = context.mapping.map(dbModel);

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
		final MultisigSignerModification dbModel = context.createDbModel();

		// Act:
		final MultisigSignerModificationTransaction model = context.mapping.map(dbModel);

		// Assert:
		context.assertModel(model, 3);
	}

	@Override
	protected MultisigSignerModification createDbModel() {
		final MultisigSignerModification transfer = new MultisigSignerModification();
		final Set<org.nem.nis.dbmodel.MultisigModification> modifications = new HashSet<>();
		modifications.add(createModification(new org.nem.nis.dbmodel.Account(), 1));
		transfer.setMultisigModifications(modifications);
		return transfer;
	}

	@Override
	protected IMapping<MultisigSignerModification, MultisigSignerModificationTransaction> createMapping(final IMapper mapper) {
		return new MultisigSignerModificationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Map<org.nem.core.model.Account, Integer> expectedModifications = new HashMap<>();
		final Set<org.nem.nis.dbmodel.MultisigModification> modifications = new HashSet<>();
		private final MultisigSignerModificationDbModelToModelMapping mapping = new MultisigSignerModificationDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, org.nem.core.model.Account.class)).thenReturn(this.sender);
		}

		private void addModification(final int type) {
			final org.nem.nis.dbmodel.Account dbCosignatory = Mockito.mock(org.nem.nis.dbmodel.Account.class);
			final org.nem.core.model.Account cosignatory = Utils.generateRandomAccount();
			Mockito.when(this.mapper.map(dbCosignatory, org.nem.core.model.Account.class)).thenReturn(cosignatory);

			this.modifications.add(createModification(dbCosignatory, type));
			this.expectedModifications.put(cosignatory, type);
		}

		public MultisigSignerModification createDbModel() {
			final MultisigSignerModification dbModification = new MultisigSignerModification();
			dbModification.setTimeStamp(4444);
			dbModification.setSender(this.dbSender);

			dbModification.setMultisigModifications(this.modifications);

			// zero out required fields
			dbModification.setFee(0L);
			dbModification.setDeadline(0);
			return dbModification;
		}

		public void assertModel(final MultisigSignerModificationTransaction model, final int numExpectedModifications) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));

			Assert.assertThat(model.getModifications().size(), IsEqual.equalTo(numExpectedModifications));
			final Map<org.nem.core.model.Account, Integer> actualModifications = new HashMap<>();
			for (final MultisigModification modification : model.getModifications()) {
				actualModifications.put(modification.getCosignatory(), modification.getModificationType().value());
			}

			Assert.assertThat(actualModifications, IsEqual.equalTo(this.expectedModifications));
		}
	}

	private static org.nem.nis.dbmodel.MultisigModification createModification(final org.nem.nis.dbmodel.Account cosignatory, final int type) {
		final org.nem.nis.dbmodel.MultisigModification dbModification = new org.nem.nis.dbmodel.MultisigModification();
		dbModification.setCosignatory(cosignatory);
		dbModification.setModificationType(type);
		return dbModification;
	}
}