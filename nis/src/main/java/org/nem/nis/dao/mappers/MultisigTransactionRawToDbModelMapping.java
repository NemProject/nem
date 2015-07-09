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
	private final Function<Long, DbMosaicCreationTransaction> mosaicCreationTransactionSupplier;
	private final Function<Long, DbSmartTileSupplyChangeTransaction> smartTileSupplyChangeTransactionSupplier;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 * @param transferSupplier Function that maps an id to a transfer.
	 * @param importanceTransferSupplier Function that maps an id to an importance transfer.
	 * @param multisigModificationTransactionSupplier Function that maps an id to a multisig aggregate modification.
	 * @param provisionNamespaceTransactionSupplier Function that maps an id to a provision namespace transaction.
	 * @param mosaicCreationTransactionSupplier Function that maps an id to a mosaic creation transaction.
	 * @param smartTileSupplyChangeTransactionSupplier Function that maps an id to a smart tile supply change transaction.
	 */
	public MultisigTransactionRawToDbModelMapping(
			final IMapper mapper,
			final Function<Long, DbTransferTransaction> transferSupplier,
			final Function<Long, DbImportanceTransferTransaction> importanceTransferSupplier,
			final Function<Long, DbMultisigAggregateModificationTransaction> multisigModificationTransactionSupplier,
			final Function<Long, DbProvisionNamespaceTransaction> provisionNamespaceTransactionSupplier,
			final Function<Long, DbMosaicCreationTransaction> mosaicCreationTransactionSupplier,
			final Function<Long, DbSmartTileSupplyChangeTransaction> smartTileSupplyChangeTransactionSupplier) {
		super(mapper);
		this.transferSupplier = transferSupplier;
		this.importanceTransferSupplier = importanceTransferSupplier;
		this.multisigModificationTransactionSupplier = multisigModificationTransactionSupplier;
		this.provisionNamespaceTransactionSupplier = provisionNamespaceTransactionSupplier;
		this.mosaicCreationTransactionSupplier = mosaicCreationTransactionSupplier;
		this.smartTileSupplyChangeTransactionSupplier = smartTileSupplyChangeTransactionSupplier;
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
		dbMultisigTransaction.setMosaicCreationTransaction(
				this.mosaicCreationTransactionSupplier.apply(RawMapperUtils.castToLong(source[15])));
		dbMultisigTransaction.setSmartTileSupplyChangeTransaction(
				this.smartTileSupplyChangeTransactionSupplier.apply(RawMapperUtils.castToLong(source[16])));
		dbMultisigTransaction.setMultisigSignatureTransactions(new HashSet<>());

		return dbMultisigTransaction;
	}
}
