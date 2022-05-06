package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

public class ImportanceTransferDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbImportanceTransferTransaction, ImportanceTransferTransaction> {

	@Test
	public void transferWithActivateModeCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbImportanceTransferTransaction dbTransfer = context.createDbTransfer(1);

		// Act:
		final ImportanceTransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, ImportanceTransferMode.Activate);
	}

	@Test
	public void transferWithDeactivateModeCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbImportanceTransferTransaction dbTransfer = context.createDbTransfer(2);

		// Act:
		final ImportanceTransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, ImportanceTransferMode.Deactivate);
	}

	@Test
	public void transferWithUnknownModeCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbImportanceTransferTransaction dbTransfer = context.createDbTransfer(3);

		// Act:
		ExceptionAssert.assertThrows(v -> context.mapping.map(dbTransfer), IllegalArgumentException.class);
	}

	@Override
	protected DbImportanceTransferTransaction createDbModel() {
		final DbImportanceTransferTransaction transfer = new DbImportanceTransferTransaction();
		transfer.setMode(1);
		return transfer;
	}

	@Override
	protected IMapping<DbImportanceTransferTransaction, ImportanceTransferTransaction> createMapping(final IMapper mapper) {
		return new ImportanceTransferDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbRemote = Mockito.mock(DbAccount.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account remote = Utils.generateRandomAccount();
		private final ImportanceTransferDbModelToModelMapping mapping = new ImportanceTransferDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRemote, Account.class)).thenReturn(this.remote);
		}

		public DbImportanceTransferTransaction createDbTransfer(final Integer mode) {
			final DbImportanceTransferTransaction dbTransfer = new DbImportanceTransferTransaction();
			dbTransfer.setSender(this.dbSender);
			dbTransfer.setRemote(this.dbRemote);
			dbTransfer.setMode(mode);
			dbTransfer.setTimeStamp(4444);

			// zero out required fields
			dbTransfer.setFee(0L);
			dbTransfer.setDeadline(0);
			return dbTransfer;
		}

		public void assertModel(final ImportanceTransferTransaction model, final ImportanceTransferMode expectedMode) {
			MatcherAssert.assertThat(model.getRemote(), IsEqual.equalTo(this.remote));
			MatcherAssert.assertThat(model.getMode(), IsEqual.equalTo(expectedMode));
		}
	}
}
