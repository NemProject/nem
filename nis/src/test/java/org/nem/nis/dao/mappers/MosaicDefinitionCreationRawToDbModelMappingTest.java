package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class MosaicDefinitionCreationRawToDbModelMappingTest
		extends
			AbstractTransferRawToDbModelMappingTest<DbMosaicDefinitionCreationTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMosaicDefinitionCreationTransaction dbModel = context.createMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(765L));
		MatcherAssert.assertThat(dbModel.getSender(), IsEqual.equalTo(context.dbSender));
		MatcherAssert.assertThat(dbModel.getMosaicDefinition(), IsEqual.equalTo(context.dbMosaicDefinition));
		MatcherAssert.assertThat(dbModel.getCreationFeeSink(), IsEqual.equalTo(context.dbCreationFeeSink));
		MatcherAssert.assertThat(dbModel.getCreationFee(), IsEqual.equalTo(654L));
	}

	@Override
	protected IMapping<Object[], DbMosaicDefinitionCreationTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaicDefinition.class))).thenReturn(new DbMosaicDefinition());
		return new MosaicDefinitionCreationRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbCreationFeeSink = Mockito.mock(DbAccount.class);
		private final DbMosaicDefinition dbMosaicDefinition = Mockito.mock(DbMosaicDefinition.class);
		private final Long senderId = 678L;
		private final Long creationFeeSinkId = 789L;
		private final Long mosaicInfo = 1337L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.creationFeeSinkId, DbAccount.class)).thenReturn(this.dbCreationFeeSink);
			Mockito.when(this.mapper.map(new Object[]{
					this.mosaicInfo
			}, DbMosaicDefinition.class)).thenReturn(this.dbMosaicDefinition);
		}

		private IMapping<Object[], DbMosaicDefinitionCreationTransaction> createMapping() {
			return new MosaicDefinitionCreationRawToDbModelMapping(this.mapper);
		}

		private Object[] createRaw() {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[15];
			raw[0] = BigInteger.valueOf(123L); // block id
			raw[1] = BigInteger.valueOf(234L); // id
			raw[2] = rawHash; // raw hash
			raw[3] = 1; // version
			raw[4] = BigInteger.valueOf(345L); // fee
			raw[5] = 456; // timestamp
			raw[6] = 567; // deadline
			raw[7] = BigInteger.valueOf(this.senderId); // sender id
			raw[8] = senderProof; // sender proof
			raw[9] = BigInteger.valueOf(543L); // mosaic id
			raw[10] = BigInteger.valueOf(this.creationFeeSinkId); // creation fee sink id
			raw[11] = BigInteger.valueOf(654L); // creation fee
			raw[12] = 432; // block index
			raw[13] = BigInteger.valueOf(765L); // referenced transaction
			raw[14] = this.mosaicInfo; // mosaic information
			return raw;
		}
	}
}
