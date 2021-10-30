package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class ImportanceTransferRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbImportanceTransferTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbImportanceTransferTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getRemote(), IsEqual.equalTo(context.dbRemote));
		MatcherAssert.assertThat(dbModel.getMode(), IsEqual.equalTo(321));
		MatcherAssert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(654L));
	}

	@Override
	protected IMapping<Object[], DbImportanceTransferTransaction> createMapping(final IMapper mapper) {
		return new ImportanceTransferRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbRemote = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;
		private final Long remoteId = 789L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.remoteId, DbAccount.class)).thenReturn(this.dbRemote);
		}

		private Object[] createRaw() {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[14];
			raw[0] = BigInteger.valueOf(123L); // block id
			raw[1] = BigInteger.valueOf(234L); // id
			raw[2] = rawHash; // raw hash
			raw[3] = 1; // version
			raw[4] = BigInteger.valueOf(345L); // fee
			raw[5] = 456; // timestamp
			raw[6] = 567; // deadline
			raw[7] = BigInteger.valueOf(this.senderId); // sender id
			raw[8] = senderProof; // sender proof
			raw[9] = BigInteger.valueOf(this.remoteId); // remote id
			raw[10] = 321; // mode
			raw[11] = 432; // block index
			raw[12] = BigInteger.valueOf(654L); // referenced transaction

			return raw;
		}
	}
}
