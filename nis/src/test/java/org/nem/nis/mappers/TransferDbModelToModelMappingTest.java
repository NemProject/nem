package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;

public class TransferDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<Transfer, TransferTransaction> {

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

	@Override
	protected Transfer createDbModel() {
		final Transfer transfer = new Transfer();
		transfer.setAmount(0L);
		return transfer;
	}

	@Override
	protected IMapping<Transfer, TransferTransaction> createMapping(final IMapper mapper) {
		return new TransferDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final org.nem.nis.dbmodel.Account dbSender = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final org.nem.nis.dbmodel.Account dbRecipient = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final TransferDbModelToModelMapping mapping = new TransferDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRecipient, Account.class)).thenReturn(this.recipient);
		}

		public Transfer createDbTransfer() {
			final Transfer dbTransfer = new Transfer();
			dbTransfer.setTimeStamp(4444);
			dbTransfer.setSender(this.dbSender);
			dbTransfer.setRecipient(this.dbRecipient);
			dbTransfer.setAmount(111111L);

			// zero out required fields
			dbTransfer.setFee(0L);
			dbTransfer.setDeadline(0);
			return dbTransfer;
		}

		public void assertModel(final TransferTransaction model) {
			Assert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(4444)));
			Assert.assertThat(model.getSigner(), IsEqual.equalTo(this.sender));
			Assert.assertThat(model.getRecipient(), IsEqual.equalTo(this.recipient));
			Assert.assertThat(model.getAmount(), IsEqual.equalTo(Amount.fromMicroNem(111111)));
		}
	}
}