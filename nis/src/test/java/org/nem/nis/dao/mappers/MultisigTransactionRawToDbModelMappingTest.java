package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.nem.core.model.TransactionTypes;
import org.nem.core.test.*;
import org.nem.nis.dao.MultisigTransferMap;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.DbTestUtils;

import java.math.BigInteger;
import java.util.Collection;

@RunWith(Enclosed.class)
public class MultisigTransactionRawToDbModelMappingTest {

	// region General

	public static class General extends AbstractTransferRawToDbModelMappingTest<DbMultisigTransaction> {

		@Override
		protected IMapping<Object[], DbMultisigTransaction> createMapping(final IMapper mapper) {
			return new MultisigTransactionRawToDbModelMapping(mapper, new MultisigTransferMap());
		}
	}

	// endregion

	// region PerTransaction

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TransactionRegistry.Entry<?, ?> entry;

		public PerTransaction(final int type) {
			this.entry = TransactionRegistry.findByType(type);
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getMultisigEmbeddableTypes());
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void rawDataCanBeMappedToDbModelWithInnerTransfer() {
			// Arrange:
			final int offset = getOffset(this.entry.type);
			final TestContext context = new TestContext();
			final Object[] raw = context.createRaw();
			raw[offset] = BigInteger.valueOf(765L);

			final AbstractBlockTransfer dbTransfer = DbTestUtils.createTransferDbModel(this.entry.dbModelClass);
			dbTransfer.setId(765L);

			final MultisigTransferMap map = new MultisigTransferMap();
			map.getEntry(this.entry.type).add(dbTransfer);

			// Act:
			final DbMultisigTransaction dbModel = new MultisigTransactionRawToDbModelMapping(context.mapper, map).map(raw);

			// Assert:
			assertDbModelFields(dbModel);
			for (final int type : TransactionTypes.getMultisigEmbeddableTypes()) {
				final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(type);
				assert null != entry;

				final AbstractBlockTransfer dbTransferFromMappedModel = entry.getFromMultisig.apply(dbModel);
				if (type == this.entry.type) {
					MatcherAssert.assertThat(dbTransferFromMappedModel, IsEqual.equalTo(dbTransfer));
				} else {
					MatcherAssert.assertThat(dbTransferFromMappedModel, IsNull.nullValue());
				}
			}
		}

		private static int getOffset(final int type) {
			int offset = 11;
			for (final int t : TransactionTypes.getMultisigEmbeddableTypes()) {
				if (t == type) {
					break;
				}

				++offset;
			}

			return offset;
		}

		private static void assertDbModelFields(final DbMultisigTransaction dbModel) {
			// Assert:
			MatcherAssert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
			MatcherAssert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
			MatcherAssert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
			MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(654L));
			MatcherAssert.assertThat(dbModel.getMultisigSignatureTransactions().isEmpty(), IsEqual.equalTo(true));
		}
	}

	// endregion

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
			final Object[] raw = new Object[17];
			raw[0] = BigInteger.valueOf(123L); // block id
			raw[1] = BigInteger.valueOf(234L); // id
			raw[2] = rawHash; // raw hash
			raw[3] = 1; // version
			raw[4] = BigInteger.valueOf(345L); // fee
			raw[5] = 456; // timestamp
			raw[6] = 567; // deadline
			raw[7] = BigInteger.valueOf(this.senderId); // sender id
			raw[8] = senderProof; // sender proof
			raw[9] = 432; // block index
			raw[10] = BigInteger.valueOf(654L); // referenced transaction
			raw[11] = null; // db transfer id
			raw[12] = null; // db importance transfer id
			raw[13] = null; // db modification transaction id
			raw[14] = null; // db provision namespace transaction id
			raw[15] = null; // db mosaic definition creation transaction id
			raw[16] = null; // db mosaic supply change transaction id
			return raw;
		}
	}
}
