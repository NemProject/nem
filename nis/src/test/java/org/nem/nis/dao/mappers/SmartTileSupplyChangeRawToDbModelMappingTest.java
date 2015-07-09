package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class SmartTileSupplyChangeRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbSmartTileSupplyChangeTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbSmartTileSupplyChangeTransaction dbModel = context.createMapping().map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(765L));
		Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(context.dbSender));
		Assert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.food"));
		Assert.assertThat(dbModel.getMosaicName(), IsEqual.equalTo("apples"));
		Assert.assertThat(dbModel.getSupplyType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(789L));
	}

	@Override
	protected IMapping<Object[], DbSmartTileSupplyChangeTransaction> createMapping(final IMapper mapper) {
		return new SmartTileSupplyChangeRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
		}

		private IMapping<Object[], DbSmartTileSupplyChangeTransaction> createMapping() {
			return new SmartTileSupplyChangeRawToDbModelMapping(this.mapper);
		}

		private Object[] createRaw() {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[15];
			raw[0] = BigInteger.valueOf(123L);                              // block id
			raw[1] = BigInteger.valueOf(234L);                              // id
			raw[2] = rawHash;                                               // raw hash
			raw[3] = 1;                                                     // version
			raw[4] = BigInteger.valueOf(345L);                              // fee
			raw[5] = 456;                                                   // timestamp
			raw[6] = 567;                                                   // deadline
			raw[7] = BigInteger.valueOf(this.senderId);                     // sender id
			raw[8] = senderProof;                                           // sender proof
			raw[9] = "alice.food";                                          // namespace id
			raw[10] = "apples";                                             // mosaic name
			raw[11] = 1;                                                    // supply type
			raw[12] = BigInteger.valueOf(789L);                             // quantity
			raw[13] = 432;                                                  // block index
			raw[14] = BigInteger.valueOf(765L);                             // referenced transaction
			return raw;
		}
	}
}
