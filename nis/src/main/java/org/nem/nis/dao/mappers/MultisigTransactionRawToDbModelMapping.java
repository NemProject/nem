package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.util.HashSet;
import java.util.function.Function;

/**
 * A mapping that is able to map raw multisig transaction data to a db multisig transaction.
 */
public class MultisigTransactionRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMultisigTransaction> {

	private final Function<Long, DbTransferTransaction> transferSupplier;
	private final Function<Long, DbImportanceTransferTransaction> importanceTransferSupplier;
	private final Function<Long, DbMultisigAggregateModificationTransaction> multisigModificationTransactionSupplier;
	private final Function<Long, DbProvisionNamespaceTransaction> provisionNamespaceTransactionSupplier;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 * @param transferSupplier Function that maps an id to a transfer.
	 * @param importanceTransferSupplier Function that maps an id to an importance transfer.
	 * @param multisigModificationTransactionSupplier Function that maps an id to a multisig aggregate modification.
	 */
	public MultisigTransactionRawToDbModelMapping(
			final IMapper mapper,
			final Function<Long, DbTransferTransaction> transferSupplier,
			final Function<Long, DbImportanceTransferTransaction> importanceTransferSupplier,
			final Function<Long, DbMultisigAggregateModificationTransaction> multisigModificationTransactionSupplier,
			final Function<Long, DbProvisionNamespaceTransaction> provisionNamespaceTransactionSupplier) {
		super(mapper);
		this.transferSupplier = transferSupplier;
		this.importanceTransferSupplier = importanceTransferSupplier;
		this.multisigModificationTransactionSupplier = multisigModificationTransactionSupplier;
		this.provisionNamespaceTransactionSupplier = provisionNamespaceTransactionSupplier;
	}

	@Override
	protected DbMultisigTransaction mapImpl(final Object[] source) {
		final DbMultisigTransaction dbMultisigTransaction = new DbMultisigTransaction();
		dbMultisigTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbMultisigTransaction.setBlkIndex((Integer)source[9]);
		dbMultisigTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[10]));
		dbMultisigTransaction.setTransferTransaction(
				this.transferSupplier.apply(RawMapperUtils.castToLong(source[11])));
		dbMultisigTransaction.setImportanceTransferTransaction(
				this.importanceTransferSupplier.apply(RawMapperUtils.castToLong(source[12])));
		dbMultisigTransaction.setMultisigAggregateModificationTransaction(
				this.multisigModificationTransactionSupplier.apply(RawMapperUtils.castToLong(source[13])));
		dbMultisigTransaction.setProvisionNamespaceTransaction(
				this.provisionNamespaceTransactionSupplier.apply(RawMapperUtils.castToLong(source[14])));
		dbMultisigTransaction.setMultisigSignatureTransactions(new HashSet<>());

		return dbMultisigTransaction;
	}
}
