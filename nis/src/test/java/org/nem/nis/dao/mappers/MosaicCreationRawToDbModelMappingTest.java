package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class MosaicCreationRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbMosaicCreationTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMosaicCreationTransaction dbModel = context.createMapping().map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(765L));
		Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(context.dbSender));
		Assert.assertThat(dbModel.getMosaic(), IsEqual.equalTo(context.dbMosaic));
	}

	@Override
	protected IMapping<Object[], DbMosaicCreationTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaic.class))).thenReturn(new DbMosaic());
		return new MosaicCreationRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbMosaic dbMosaic = Mockito.mock(DbMosaic.class);
		private final Long senderId = 678L;
		private final Long mosaicInfo = 1337L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(new Object[] { this.mosaicInfo }, DbMosaic.class)).thenReturn(this.dbMosaic);
		}

		private IMapping<Object[], DbMosaicCreationTransaction> createMapping() {
			return new MosaicCreationRawToDbModelMapping(this.mapper);
		}

		private Object[] createRaw() {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[13];
			raw[0] = BigInteger.valueOf(123L);                              // block id
			raw[1] = BigInteger.valueOf(234L);                              // id
			raw[2] = rawHash;                                               // raw hash
			raw[3] = 1;                                                     // version
			raw[4] = BigInteger.valueOf(345L);                              // fee
			raw[5] = 456;                                                   // timestamp
			raw[6] = 567;                                                   // deadline
			raw[7] = BigInteger.valueOf(this.senderId);                     // sender id
			raw[8] = senderProof;                                           // sender proof
			raw[9] = BigInteger.valueOf(543L);                              // mosaic id
			raw[10] = 432;                                                  // block index
			raw[11] = BigInteger.valueOf(765L);                             // referenced transaction
			raw[12] = this.mosaicInfo;                                      // mosaic information
			return raw;
		}
	}
}
