package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ImportanceTransferModelToDbModelMappingTest {
	@Test
	public void transferWithActivateModeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transfer = context.createModel(ImportanceTransferTransaction.Mode.Activate);

		// Act:
		final ImportanceTransfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, 1, HashUtils.calculateHash(transfer));
	}

	@Test
	public void transferWithDeactivateModeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transfer = context.createModel(ImportanceTransferTransaction.Mode.Deactivate);

		// Act:
		final ImportanceTransfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, 2, HashUtils.calculateHash(transfer));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbRemote = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account remote = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final ImportanceTransferModelToDbModelMapping mapping = new ImportanceTransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.sender, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.remote, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbRemote);
		}

		public ImportanceTransferTransaction createModel(final ImportanceTransferTransaction.Mode mode) {
			final ImportanceTransferTransaction transfer = new ImportanceTransferTransaction(
					new TimeInstant(4444),
					this.sender,
					mode,
					this.remote);

			transfer.setFee(Amount.fromMicroNem(98765432L));
			transfer.setDeadline(new TimeInstant(123));
			transfer.setSignature(this.signature);
			return transfer;
		}

		public void assertDbModel(
				final ImportanceTransfer dbModel,
				final Integer expectedMode,
				final Hash expectedHash) {
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getType(), IsEqual.equalTo(TransactionTypes.IMPORTANCE_TRANSFER));

			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getRemote(), IsEqual.equalTo(this.dbRemote));
			Assert.assertThat(dbModel.getMode(), IsEqual.equalTo(expectedMode));

			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(98765432L));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(123));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.signature.getBytes()));

			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(expectedHash));
		}
	}
}