package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.*;

public class TransferModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<TransferTransaction, DbTransferTransaction> {

	@Override
	protected int getVersion() {
		return VerifiableEntityUtils.VERSION_TWO;
	}

	@Test
	public void transferWithNoMessageAndNoSmartTilesCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(null);

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMessagePayload(), IsNull.nullValue());
		Assert.assertThat(dbModel.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithPlainMessageCanBeMappedToDbModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(new PlainMessage(messagePayload));

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		Assert.assertThat(dbModel.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithSecureMessageCanBeMappedToDbModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(SecureMessage.fromEncodedPayload(context.sender, context.recipient, messagePayload));

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(2));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		Assert.assertThat(dbModel.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithPlainMessageAndSmartTilesCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TransferTransaction transfer = context.createModel(new PlainMessage(messagePayload), new SmartTileBag(context.smartTiles));

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		Assert.assertThat(dbModel.getSmartTiles().size(), IsEqual.equalTo(5));
		Assert.assertThat(dbModel.getSmartTiles(), IsEquivalent.equivalentTo(context.dbSmartTiles));

		context.smartTiles.forEach(smartTiles ->
				Mockito.verify(context.mapper, Mockito.times(1)).map(smartTiles, DbSmartTile.class));
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
	protected TransferModelToDbModelMapping createMapping(final IMapper mapper) {
		return new TransferModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbRecipient = Mockito.mock(DbAccount.class);
		private final Account sender = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private List<SmartTile> smartTiles = IntStream.range(0, 5).mapToObj(Utils::createSmartTile).collect(Collectors.toList());
		private List<DbSmartTile> dbSmartTiles = IntStream.range(0, 5).mapToObj(i -> Mockito.mock(DbSmartTile.class)).collect(Collectors.toList());
		private final TransferModelToDbModelMapping mapping = new TransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.recipient, DbAccount.class)).thenReturn(this.dbRecipient);
			IntStream.range(0, 5).forEach(i -> Mockito.when(this.mapper.map(smartTiles.get(i), DbSmartTile.class)).thenReturn(dbSmartTiles.get(i)));
		}

		public TransferTransaction createModel(final Message message) {
			return this.createModel(message, new SmartTileBag(Collections.emptyList()));
		}

		public TransferTransaction createModel(final Message message, final SmartTileBag bag) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					this.sender,
					this.recipient,
					Amount.fromMicroNem(111111),
					message,
					bag);
		}

		public void assertDbModel(final DbTransferTransaction dbModel, final TransferTransaction model) {
			Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(this.dbRecipient));
			Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(111111L));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(model)));
		}
	}
}