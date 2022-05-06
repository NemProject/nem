package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class ProvisionNamespaceRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbProvisionNamespaceTransaction> {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbProvisionNamespaceTransaction dbModel = context.createMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(765L));
		MatcherAssert.assertThat(dbModel.getSender(), IsEqual.equalTo(context.dbSender));
		MatcherAssert.assertThat(dbModel.getRentalFeeSink(), IsEqual.equalTo(context.dbRentalFeeSink));
		MatcherAssert.assertThat(dbModel.getRentalFee(), IsEqual.equalTo(654L));
		MatcherAssert.assertThat(dbModel.getNamespace(), IsEqual.equalTo(context.dbNamespace));
	}

	@Override
	protected IMapping<Object[], DbProvisionNamespaceTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbNamespace.class))).thenReturn(new DbNamespace());
		return new ProvisionNamespaceRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbNamespace dbNamespace = Mockito.mock(DbNamespace.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final DbAccount dbRentalFeeSink = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;
		private final Long rentalFeeSinkId = 789L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
			Mockito.when(this.mapper.map(this.rentalFeeSinkId, DbAccount.class)).thenReturn(this.dbRentalFeeSink);
			Mockito.when(this.mapper.map(Mockito.any(), Mockito.eq(DbNamespace.class))).thenReturn(this.dbNamespace);
		}

		private IMapping<Object[], DbProvisionNamespaceTransaction> createMapping() {
			return new ProvisionNamespaceRawToDbModelMapping(this.mapper);
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
			raw[9] = BigInteger.valueOf(this.rentalFeeSinkId); // rental fee sink id
			raw[10] = BigInteger.valueOf(654L); // rental fee
			raw[11] = 5; // namespace id
			raw[12] = 432; // block index
			raw[13] = BigInteger.valueOf(765L); // referenced transaction
			return raw;
		}
	}
}
