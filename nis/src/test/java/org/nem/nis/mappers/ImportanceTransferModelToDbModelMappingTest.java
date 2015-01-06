package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ImportanceTransferModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<ImportanceTransferTransaction, DbImportanceTransferTransaction> {

	@Test
	public void transferWithActivateModeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transfer = context.createModel(ImportanceTransferTransaction.Mode.Activate);

		// Act:
		final DbImportanceTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, 1, transfer);
	}

	@Test
	public void transferWithDeactivateModeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transfer = context.createModel(ImportanceTransferTransaction.Mode.Deactivate);

		// Act:
		final DbImportanceTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, 2, transfer);
	}

	@Override
	protected ImportanceTransferTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new ImportanceTransferTransaction(
				timeStamp,
				sender,
				ImportanceTransferTransaction.Mode.Activate,
				Utils.generateRandomAccount());
	}

	@Override
	protected ImportanceTransferModelToDbModelMapping createMapping(final IMapper mapper) {
		return new ImportanceTransferModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbRemote = Mockito.mock(DbAccount.class);
		private final Account remote = Utils.generateRandomAccount();
		private final ImportanceTransferModelToDbModelMapping mapping = new ImportanceTransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.remote, DbAccount.class)).thenReturn(this.dbRemote);
		}

		public ImportanceTransferTransaction createModel(final ImportanceTransferTransaction.Mode mode) {
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					Utils.generateRandomAccount(),
					mode,
					this.remote);
		}

		public void assertDbModel(
				final DbImportanceTransferTransaction dbModel,
				final Integer expectedMode,
				final ImportanceTransferTransaction model) {
			Assert.assertThat(dbModel.getRemote(), IsEqual.equalTo(this.dbRemote));
			Assert.assertThat(dbModel.getMode(), IsEqual.equalTo(expectedMode));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(model)));
		}
	}
}