package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MultisigAggregateModificationModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> {

	@Override
	protected int getVersion() {
		return VerifiableEntityUtils.VERSION_TWO;
	}

	@Test
	public void transferWithSingleCosignatoryModificationAndWithoutMinCosignatoriesModificationCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 1, 0);
	}

	@Test
	public void transferWithSingleCosignatoryModificationAndWithMinCosignatoriesModificationCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		context.setMinCosignatoriesModification(new MultisigMinCosignatoriesModification(5));
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 1, 5);
	}

	@Test
	public void transferWithMultipleCosignatoryModificationsAndWithoutMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		context.addModification(2);
		context.addModification(1);
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 3, 0);
	}

	@Test
	public void transferWithMultipleCosignatoryModificationsAndWithMinCosignatoriesModificationCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addModification(1);
		context.addModification(2);
		context.addModification(1);
		context.setMinCosignatoriesModification(new MultisigMinCosignatoriesModification(5));
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 3, 5);
	}

	@Test
	public void transferWithoutCosignatoryModificationAndWithMinCosignatoriesModificationCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setMinCosignatoriesModification(new MultisigMinCosignatoriesModification(5));
		final MultisigAggregateModificationTransaction model = context.createModel();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = context.mapping.map(model);

		// Assert:
		context.assertModel(dbModel, 0, 5);
	}

	@Override
	protected MultisigAggregateModificationTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new MultisigAggregateModificationTransaction(timeStamp, sender, Collections.singletonList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())));
	}

	@Override
	protected IMapping<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> createMapping(
			final IMapper mapper) {
		return new MultisigAggregateModificationModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final org.nem.core.model.Account sender = Utils.generateRandomAccount();
		private final Map<DbAccount, Integer> expectedModifications = new HashMap<>();
		final List<MultisigCosignatoryModification> cosignatoryModifications = new ArrayList<>();
		MultisigMinCosignatoriesModification minCosignatoriesModification;
		private final MultisigAggregateModificationModelToDbModelMapping mapping = new MultisigAggregateModificationModelToDbModelMapping(
				this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.sender, DbAccount.class)).thenReturn(this.dbSender);
		}

		private void addModification(final int type) {
			final DbAccount dbCosignatory = Mockito.mock(DbAccount.class);
			final org.nem.core.model.Account cosignatory = Utils.generateRandomAccount();
			Mockito.when(this.mapper.map(cosignatory, DbAccount.class)).thenReturn(dbCosignatory);

			this.cosignatoryModifications.add(createModification(cosignatory, type));
			this.expectedModifications.put(dbCosignatory, type);
		}

		private void setMinCosignatoriesModification(final MultisigMinCosignatoriesModification minCosignatoriesModification) {
			this.minCosignatoriesModification = minCosignatoriesModification;
		}

		public MultisigAggregateModificationTransaction createModel() {
			return new MultisigAggregateModificationTransaction(TimeInstant.ZERO, this.sender, this.cosignatoryModifications,
					this.minCosignatoriesModification);
		}

		public void assertModel(final DbMultisigAggregateModificationTransaction dbModel, final int numExpectedModifications,
				final int expectedRelativeChange) {
			MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			MatcherAssert.assertThat(dbModel.getMultisigModifications().size(), IsEqual.equalTo(numExpectedModifications));
			final Map<DbAccount, Integer> actualModifications = new HashMap<>();
			for (final DbMultisigModification modification : dbModel.getMultisigModifications()) {
				actualModifications.put(modification.getCosignatory(), modification.getModificationType());
			}

			MatcherAssert.assertThat(actualModifications, IsEqual.equalTo(this.expectedModifications));
			for (final DbMultisigModification modification : dbModel.getMultisigModifications()) {
				MatcherAssert.assertThat(modification.getMultisigAggregateModificationTransaction(), IsEqual.equalTo(dbModel));
			}

			final DbMultisigMinCosignatoriesModification dbMinCosignatoriesModification = dbModel.getMultisigMinCosignatoriesModification();
			if (0 != expectedRelativeChange) {
				MatcherAssert.assertThat(dbMinCosignatoriesModification.getRelativeChange(), IsEqual.equalTo(expectedRelativeChange));
			} else {
				MatcherAssert.assertThat(dbMinCosignatoriesModification, IsNull.nullValue());
			}
		}
	}

	private static MultisigCosignatoryModification createModification(final Account cosignatory, final int type) {
		return new MultisigCosignatoryModification(MultisigModificationType.fromValueOrDefault(type), cosignatory);
	}
}
