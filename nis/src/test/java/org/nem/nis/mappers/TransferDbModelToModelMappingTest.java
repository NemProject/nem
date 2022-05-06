package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class TransferDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbTransferTransaction, TransferTransaction> {

	@Test
	public void transferWithNoMessageAndNoMosaicsCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransferTransaction = context.createDbTransfer();

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransferTransaction);

		// Assert:
		context.assertModel(model);
		MatcherAssert.assertThat(model.getMessage(), IsNull.nullValue());
		MatcherAssert.assertThat(model.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithPlainMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(1);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		MatcherAssert.assertThat(model.getMessage(), IsNull.notNullValue());
		MatcherAssert.assertThat(model.getMessage().getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(model.getMessage().getEncodedPayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(model.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithSecureMessageCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(2);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		MatcherAssert.assertThat(model.getMessage(), IsNull.notNullValue());
		MatcherAssert.assertThat(model.getMessage().getType(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(model.getMessage().getEncodedPayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(model.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithUnknownMessageCannotBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(3);
		dbTransfer.setMessagePayload(messagePayload);

		// Act:
		ExceptionAssert.assertThrows(v -> context.mapping.map(dbTransfer), IllegalArgumentException.class);
	}

	@Test
	public void transferWithPlainMessageAndMosaicsCanBeMappedToModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final DbTransferTransaction dbTransfer = context.createDbTransfer();
		dbTransfer.setMessageType(1);
		dbTransfer.setMessagePayload(messagePayload);
		dbTransfer.getMosaics().addAll(context.dbMosaics);

		// Act:
		final TransferTransaction model = context.mapping.map(dbTransfer);

		// Assert:
		context.assertModel(model);
		MatcherAssert.assertThat(model.getMessage(), IsNull.notNullValue());
		MatcherAssert.assertThat(model.getMessage().getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(model.getMessage().getEncodedPayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(model.getAttachment().getMosaics(), IsEquivalent.equivalentTo(context.mosaics));

		context.dbMosaics.forEach(dbMosaic -> Mockito.verify(context.mapper, Mockito.times(1)).map(dbMosaic, Mosaic.class));
	}

	@Override
	protected DbTransferTransaction createDbModel() {
		final DbTransferTransaction transfer = new DbTransferTransaction();
		transfer.setAmount(0L);
		return transfer;
	}

	@Override
	protected IMapping<DbTransferTransaction, TransferTransaction> createMapping(final IMapper mapper) {
		return new TransferDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbRecipient = Mockito.mock(DbAccount.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final List<Mosaic> mosaics = new ArrayList<>();
		private final List<DbMosaic> dbMosaics = new ArrayList<>();
		private final TransferDbModelToModelMapping mapping = new TransferDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbRecipient, Account.class)).thenReturn(this.recipient);
			for (int i = 0; i < 5; ++i) {
				this.mosaics.add(Utils.createMosaic(i));
				this.dbMosaics.add(Mockito.mock(DbMosaic.class));
				Mockito.when(this.mapper.map(this.dbMosaics.get(i), Mosaic.class)).thenReturn(this.mosaics.get(i));
			}
		}

		public DbTransferTransaction createDbTransfer() {
			final DbTransferTransaction dbTransfer = new DbTransferTransaction();
			dbTransfer.setVersion(2);
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
			MatcherAssert.assertThat(model.getRecipient(), IsEqual.equalTo(this.recipient));
			MatcherAssert.assertThat(model.getAmount(), IsEqual.equalTo(Amount.fromMicroNem(111111)));
		}
	}
}
