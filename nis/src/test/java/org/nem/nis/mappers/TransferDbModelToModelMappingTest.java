package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;

public class TransferDbModelToModelMappingTest {

	@Test
	public void transferWithNoMessageCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transfer dbTransfer = context.createDbTransfer();

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getMessage(), IsNull.nullValue());
	}

	@Test
	public void transferWithPlainMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final Transfer dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(1);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getMessage(), IsNull.notNullValue());
		Assert.assertThat(model.getMessage().getType(), IsEqual.equalTo(1));
		Assert.assertThat(model.getMessage().getEncodedPayload(), IsEqual.equalTo(messagePayload));
	}

	@Test
	public void transferWithSecureMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final Transfer dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(2);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		Assert.assertThat(model.getMessage(), IsNull.notNullValue());
		Assert.assertThat(model.getMessage().getType(), IsEqual.equalTo(2));
		Assert.assertThat(model.getMessage().getEncodedPayload(), IsEqual.equalTo(messagePayload));
	}

	@Test
	public void transferWithUnknownMessageCannotBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final Transfer dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(3);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		ExceptionAssert.assertThrows(v -> context.mapping.map(dbTransfer), IllegalArgumentException.class);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbRecipient = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Signature signature = Utils.generateRandomSignature();
		private final TransferDbModelToModelMapping mapping = new TransferDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRecipient, Account.class)).thenReturn(this.recipient);
		}

		public Transfer createDbTransfer() {
			final Transfer dbTransfer = new Transfer();
			dbTransfer.setSender(this.dbSender);
			dbTransfer.setRecipient(this.dbRecipient);
			dbTransfer.setAmount(111111L);

			dbTransfer.setFee(98765432L);
			dbTransfer.setTimeStamp(4444);
			dbTransfer.setDeadline(123);
			dbTransfer.setSenderProof(this.signature.getBytes());
			return dbTransfer;
		}

		public void assertModel(final TransferTransaction model) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));
			Assert.assertThat(model.getRecipient(), IsEqual.equalTo(this.recipient));
			Assert.assertThat(model.getAmount(), IsEqual.equalTo(Amount.fromMicroNem(111111)));

			Assert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(98765432)));
			Assert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(123)));
			Assert.assertThat(model.getSignature(), IsEqual.equalTo(this.signature));
		}
	}
}