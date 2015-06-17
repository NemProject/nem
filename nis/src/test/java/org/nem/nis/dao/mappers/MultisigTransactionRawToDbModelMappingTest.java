package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.HashMap;

public class MultisigTransactionRawToDbModelMappingTest extends AbstractTransferRawToDbModelMappingTest<DbMultisigTransaction> {
	private static final HashMap<Long, DbTransferTransaction> DB_TRANSFER_MAP = new HashMap<>();
	private static final HashMap<Long, DbImportanceTransferTransaction> DB_IMPORTANCE_TRANSFER_MAP = new HashMap<>();
	private static final HashMap<Long, DbMultisigAggregateModificationTransaction> DB_MODIFICATION_TRANSACTION_MAP = new HashMap<>();
	private static final HashMap<Long, DbProvisionNamespaceTransaction> DB_PROVISION_NAMESPACE_TRANSACTION_MAP = new HashMap<>();

	@Before
	public void clearMaps() {
		DB_TRANSFER_MAP.clear();
		DB_IMPORTANCE_TRANSFER_MAP.clear();
		DB_MODIFICATION_TRANSACTION_MAP.clear();
	}

	@Test
	public void rawDataCanBeMappedToDbModelWithTransferTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(BigInteger.valueOf(765L), null, null, null);
		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setId(765L);
		DB_TRANSFER_MAP.put(765L, dbTransfer);

		// Act:
		final DbMultisigTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel);
		Assert.assertThat(dbModel.getTransferTransaction(), IsEqual.equalTo(dbTransfer));
		Assert.assertThat(dbModel.getImportanceTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMultisigAggregateModificationTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getProvisionNamespaceTransaction(), IsNull.nullValue());
	}

	@Test
	public void rawDataCanBeMappedToDbModelWithImportanceTransferTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(null, BigInteger.valueOf(876L), null, null);
		final DbImportanceTransferTransaction dbImportanceTransfer = new DbImportanceTransferTransaction();
		dbImportanceTransfer.setId(876L);
		DB_IMPORTANCE_TRANSFER_MAP.put(876L, dbImportanceTransfer);

		// Act:
		final DbMultisigTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel);
		Assert.assertThat(dbModel.getTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getImportanceTransferTransaction(), IsEqual.equalTo(dbImportanceTransfer));
		Assert.assertThat(dbModel.getMultisigAggregateModificationTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getProvisionNamespaceTransaction(), IsNull.nullValue());
	}

	@Test
	public void rawDataCanBeMappedToDbModelWithModificationTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(null, null, BigInteger.valueOf(987L), null);
		final DbMultisigAggregateModificationTransaction dbModificationTransaction = new DbMultisigAggregateModificationTransaction();
		dbModificationTransaction.setId(987L);
		DB_MODIFICATION_TRANSACTION_MAP.put(987L, dbModificationTransaction);

		// Act:
		final DbMultisigTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel);
		Assert.assertThat(dbModel.getTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getImportanceTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMultisigAggregateModificationTransaction(), IsEqual.equalTo(dbModificationTransaction));
		Assert.assertThat(dbModel.getProvisionNamespaceTransaction(), IsNull.nullValue());
	}

	@Test
	public void rawDataCanBeMappedToDbModelWithProvisionNamespaceTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(null, null, null, BigInteger.valueOf(876L));
		final DbProvisionNamespaceTransaction dbProvisionNamespaceTransaction = new DbProvisionNamespaceTransaction();
		dbProvisionNamespaceTransaction.setId(876L);
		DB_PROVISION_NAMESPACE_TRANSACTION_MAP.put(876L, dbProvisionNamespaceTransaction);

		// Act:
		final DbMultisigTransaction dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(dbModel);
		Assert.assertThat(dbModel.getTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getImportanceTransferTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getMultisigAggregateModificationTransaction(), IsNull.nullValue());
		Assert.assertThat(dbModel.getProvisionNamespaceTransaction(), IsEqual.equalTo(dbProvisionNamespaceTransaction));
	}

	private void assertDbModelFields(final DbMultisigTransaction dbModel) {
		// Assert:
		Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlock().getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(432));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(654L));
	}

	@Override
	protected IMapping<Object[], DbMultisigTransaction> createMapping(final IMapper mapper) {
		return new MultisigTransactionRawToDbModelMapping(
				mapper,
				DB_TRANSFER_MAP::get,
				DB_IMPORTANCE_TRANSFER_MAP::get,
				DB_MODIFICATION_TRANSACTION_MAP::get,
				DB_PROVISION_NAMESPACE_TRANSACTION_MAP::get);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Long senderId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.senderId, DbAccount.class)).thenReturn(this.dbSender);
		}

		private Object[] createRaw(
				final BigInteger dbTransferId,
				final BigInteger dbImportanceTransferId,
				final BigInteger dbModificationTransactionId,
				final BigInteger dbProvisionNamespaceTransactionId) {
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
			raw[9] = 432;                                                   // block index
			raw[10] = BigInteger.valueOf(654L);                             // referenced transaction
			raw[11] = dbTransferId;                                         // db transfer id
			raw[12] = dbImportanceTransferId;                               // db importance transfer id
			raw[13] = dbModificationTransactionId;                          // db modification transaction id
			raw[14] = dbProvisionNamespaceTransactionId;                    // db provision namespace transaction id

			return raw;
		}
	}
}
