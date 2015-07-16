package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class TransferRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbTransferTransaction> {

	// TODO 20150715 J-B: dumb question, i guess we have to update the mapper, right?

	@Test
	public void rawWithNoMessageAndNoSmartTileCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(null);

		// Act:
		final DbTransferTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel, context.dbRecipient);
		Assert.assertThat(dbModel.getMessageType(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMessagePayload(), IsNull.nullValue());
		Assert.assertThat(dbModel.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void rawWithMessageAndNoSmartTileCanBeMappedToDbModel() {
		// Arrange:
		final byte[] message = Utils.generateRandomBytes();
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(message);

		// Act:
		final DbTransferTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel, context.dbRecipient);
		Assert.assertThat(dbModel.getMessageType(), IsEqual.equalTo(765));
		Assert.assertThat(dbModel.getMessagePayload(), IsEqual.equalTo(message));
		Assert.assertThat(dbModel.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	private void assertDbModelFields(final DbTransferTransaction dbModel, final DbAccount dbRecipient) {
		// Assert:
		Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(dbRecipient));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(321));
		Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(543L));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(654L));
	}

	@Override
	protected IMapping<Object[], DbTransferTransaction> createMapping(final IMapper mapper) {
		return new TransferRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbRecipient = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;
		private final Long recipientId = 789L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.recipientId, DbAccount.class)).thenReturn(this.dbRecipient);
		}

		private Object[] createRaw(final byte[] message) {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[16];
			raw[0] = BigInteger.valueOf(123L);                              // block id
			raw[1] = BigInteger.valueOf(234L);                              // id
			raw[2] = rawHash;                                               // raw hash
			raw[3] = 1;                                                     // version
			raw[4] = BigInteger.valueOf(345L);                              // fee
			raw[5] = 456;                                                   // timestamp
			raw[6] = 567;                                                   // deadline
			raw[7] = BigInteger.valueOf(this.senderId);                     // sender id
			raw[8] = senderProof;                                           // sender proof
			raw[9] = BigInteger.valueOf(this.recipientId);                  // recipient id
			raw[10] = 321;                                                  // block index
			raw[11] = BigInteger.valueOf(543L);                             // amount
			raw[12] = BigInteger.valueOf(654L);                             // referenced transaction
			raw[13] = null == message ? null : 765;                         // message type
			raw[14] = message;                                              // raw message bytes

			return raw;
		}
	}
}
