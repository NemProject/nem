package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class TransferModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<TransferTransaction, Transfer> {

	@Test
	public void transferWithNoMessageCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(null);

		// Act:
		final Transfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMessagePayload(), IsNull.nullValue());
	}

	@Test
	public void transferWithPlainMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(new PlainMessage(messagePayload));

		// Act:
		final Transfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
	}

	@Test
	public void transferWithSecureMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(SecureMessage.fromEncodedPayload(context.sender, context.recipient, messagePayload));

		// Act:
		final Transfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(2));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
	}

	@Override
	protected TransferTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new TransferTransaction(
				timeStamp,
				sender,
				Utils.generateRandomAccount(),
				Amount.fromMicroNem(111111),
				null);
	}

	@Override
	protected Transfer map(final TransferTransaction model, final IMapper mapper) {
		return new TransferModelToDbModelMapping(mapper).map(model);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbRecipient = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final TransferModelToDbModelMapping mapping = new TransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.recipient, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbRecipient);
		}

		public TransferTransaction createModel(final Message message) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					this.sender,
					this.recipient,
					Amount.fromMicroNem(111111),
					message);
		}

		public void assertDbModel(final Transfer dbModel, final TransferTransaction model) {
			Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(this.dbRecipient));
			Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(111111L));
			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(model)));
		}
	}
}