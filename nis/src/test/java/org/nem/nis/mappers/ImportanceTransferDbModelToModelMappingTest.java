package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ImportanceTransferDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<ImportanceTransfer, ImportanceTransferTransaction> {

	@Test
	public void transferWithActivateModeCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransfer dbTransfer = context.createDbTransfer(1);

		// Act:
		final ImportanceTransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void transferWithDeactivateModeCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransfer dbTransfer = context.createDbTransfer(2);

		// Act:
		final ImportanceTransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model, ImportanceTransferTransaction.Mode.Deactivate);
	}

	@Test
	public void transferWithUnknownModeCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransfer dbTransfer = context.createDbTransfer(3);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.mapping.map(dbTransfer),
				IllegalArgumentException.class);
	}

	@Override
	protected ImportanceTransfer createDbModel() {
		final ImportanceTransfer transfer = new ImportanceTransfer();
		transfer.setMode(1);
		return transfer;
	}

	@Override
	protected IMapping<ImportanceTransfer, ImportanceTransferTransaction> createMapping(final IMapper mapper) {
		return new ImportanceTransferDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbRemote = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account remote = Utils.generateRandomAccount();
		private final ImportanceTransferDbModelToModelMapping mapping = new ImportanceTransferDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRemote, Account.class)).thenReturn(this.remote);
		}

		public ImportanceTransfer createDbTransfer(final Integer mode) {
			final ImportanceTransfer dbTransfer = new ImportanceTransfer();
			dbTransfer.setSender(this.dbSender);
			dbTransfer.setRemote(this.dbRemote);
			dbTransfer.setMode(mode);
			dbTransfer.setTimeStamp(4444);

			dbTransfer.setFee(0L);
			dbTransfer.setDeadline(0);
			return dbTransfer;
		}

		public void assertModel(
				final ImportanceTransferTransaction model,
				final ImportanceTransferTransaction.Mode expectedMode) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));
			Assert.assertThat(model.getRemote(), IsEqual.equalTo(this.remote));
			Assert.assertThat(model.getMode(), IsEqual.equalTo(expectedMode));
		}
	}
}