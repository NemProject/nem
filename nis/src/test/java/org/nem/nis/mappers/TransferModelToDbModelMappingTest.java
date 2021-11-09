package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class TransferModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<TransferTransaction, DbTransferTransaction> {

	@Before
	public void setup() {
		Utils.setupGlobals();
	}

	@After
	public void destroy() {
		Utils.resetGlobals();
	}

	@Override
	protected int getVersion() {
		return VerifiableEntityUtils.VERSION_TWO;
	}

	@Test
	public void transferWithNoMessageAndNoMosaicsCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context.createModel(null);

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		MatcherAssert.assertThat(dbModel.getMessageType(), IsNull.nullValue());
		MatcherAssert.assertThat(dbModel.getMessagePayload(), IsNull.nullValue());
		MatcherAssert.assertThat(dbModel.getMosaics().isEmpty(), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(dbModel.getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithSecureMessageCanBeMappedToDbModel() {
		// Arrange:
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final TransferTransaction transfer = context
				.createModel(SecureMessage.fromEncodedPayload(context.sender, context.recipient, messagePayload));

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		MatcherAssert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(dbModel.getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transferWithPlainMessageAndMosaicsCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final byte[] messagePayload = Utils.generateRandomBytes();
		final TransferTransaction transfer = context.createModel(new PlainMessage(messagePayload), context.mosaics);

		// Act:
		final DbTransferTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		context.assertDbModel(dbModel, transfer);
		MatcherAssert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(messagePayload));
		MatcherAssert.assertThat(dbModel.getMosaics().size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(dbModel.getMosaics(), IsEquivalent.equivalentTo(context.dbMosaics));

		context.mosaics.forEach(mosaic -> Mockito.verify(context.mapper, Mockito.times(1)).map(mosaic, DbMosaic.class));
		dbModel.getMosaics().forEach(dbMosaic -> MatcherAssert.assertThat(dbMosaic.getTransferTransaction(), IsEqual.equalTo(dbModel)));
	}

	@Override
	protected TransferTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new TransferTransaction(timeStamp, sender, Utils.generateRandomAccount(), Amount.fromMicroNem(111111), null);
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
		private final List<Mosaic> mosaics = new ArrayList<>();
		private final List<DbMosaic> dbMosaics = new ArrayList<>();
		private final TransferModelToDbModelMapping mapping = new TransferModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.recipient, DbAccount.class)).thenReturn(this.dbRecipient);
			for (int i = 0; i < 5; ++i) {
				final DbMosaic dbMosaic = new DbMosaic();
				dbMosaic.setId((long) i);
				this.mosaics.add(Utils.createMosaic(i));
				this.dbMosaics.add(dbMosaic);
				Mockito.when(this.mapper.map(this.mosaics.get(i), DbMosaic.class)).thenReturn(this.dbMosaics.get(i));
			}
		}

		public TransferTransaction createModel(final Message message) {
			return this.createModel(message, Collections.emptyList());
		}

		public TransferTransaction createModel(final Message message, final Collection<Mosaic> mosaics) {
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);
			mosaics.forEach(attachment::addMosaic);

			return new TransferTransaction(TimeInstant.ZERO, this.sender, this.recipient, Amount.fromMicroNem(111111), attachment);
		}

		public void assertDbModel(final DbTransferTransaction dbModel, final TransferTransaction model) {
			MatcherAssert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(this.dbRecipient));
			MatcherAssert.assertThat(dbModel.getAmount(), IsEqual.equalTo(111111L));
			MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));

			MatcherAssert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(model)));
		}
	}
}
