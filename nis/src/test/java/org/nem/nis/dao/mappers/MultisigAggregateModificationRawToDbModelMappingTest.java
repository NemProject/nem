package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class MultisigAggregateModificationRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbMultisigAggregateModificationTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMultisigAggregateModificationTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(654L));
		Assert.assertThat(dbModel.getMultisigModifications(), IsNull.notNullValue());
	}

	@Override
	protected IMapping<Object[], DbMultisigAggregateModificationTransaction> createMapping(final IMapper mapper) {
		return new MultisigAggregateModificationRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
		}

		private Object[] createRaw() {
			final byte[] rawHash = Utils.generateRandomBytes(32);
			final byte[] senderProof = Utils.generateRandomBytes(32);
			final Object[] raw = new Object[12];
			raw[0] = BigInteger.valueOf(123L);                              // block id
			raw[1] = BigInteger.valueOf(234L);                              // id
			raw[2] = rawHash;                                               // raw hash
			raw[3] = 1;                                                     // version
			raw[4] = BigInteger.valueOf(345L);                              // fee
			raw[5] = 456;                                                   // timestamp
			raw[6] = 567;                                                   // deadline
			raw[7] = BigInteger.valueOf(this.senderId);                     // sender id
			raw[8] = senderProof;                                           // sender proof
			raw[9] = 432;                                                   // block index
			raw[10] = 543;                                                  // order id
			raw[11] = BigInteger.valueOf(654L);                             // referenced transaction

			return raw;
		}
	}
}
