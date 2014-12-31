package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;

public class TransferModelToDbModelMappingTest {

	@Test
	public void transferWithNoMessageCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(null);

		// Act:
		final Transfer dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, HashUtils.calculateHash(transfer));
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
		context.assertDbModel(dbModel, HashUtils.calculateHash(transfer));
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
		context.assertDbModel(dbModel, HashUtils.calculateHash(transfer));
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(2));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbRecipient = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final TransferModelToDbModelMapping mapping = new TransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.sender, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.recipient, org.nem.nis.dbmodel.Account.class)).thenReturn(this.dbRecipient);
		}

		public TransferTransaction createModel(final Message message) {
			final TransferTransaction transfer = new TransferTransaction(
					new TimeInstant(4444),
					this.sender,
					this.recipient,
					Amount.fromMicroNem(111111),
					message);

			transfer.setFee(Amount.fromMicroNem(98765432L));
			transfer.setDeadline(new TimeInstant(123));
			transfer.setSignature(this.signature);
			return transfer;
		}

		public void assertDbModel(final Transfer dbModel, final Hash expectedHash) {
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));

			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(4444));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(this.dbRecipient));
			Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(111111L));

			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(98765432L));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(123));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.signature.getBytes()));

			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(expectedHash));
		}
	}
}