package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.function.Function;

/**
 * A mapping that is able to map raw multisig transaction data to a db multisig transaction.
 */
public class MultisigTransactionRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMultisigTransaction> {

	private final Function<Long, DbTransferTransaction> transferSupplier;
	private final Function<Long, DbImportanceTransferTransaction> importanceTransferSupplier;
	private final Function<Long, DbMultisigAggregateModificationTransaction> multisigModificationTransactionSupplier;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionRawToDbModelMapping(
			final IMapper mapper,
			final Function<Long, DbTransferTransaction> transferSupplier,
			final Function<Long, DbImportanceTransferTransaction> importanceTransferSupplier,
			final Function<Long, DbMultisigAggregateModificationTransaction> multisigModificationTransactionSupplier) {
		super(mapper);
		this.transferSupplier = transferSupplier;
		this.importanceTransferSupplier = importanceTransferSupplier;
		this.multisigModificationTransactionSupplier = multisigModificationTransactionSupplier;
	}

	@Override
	protected DbMultisigTransaction mapImpl(final Object[] source) {
		final DbMultisigTransaction dbMultsigTransaction = new DbMultisigTransaction();
		dbMultsigTransaction.setBlock(mapBlock(castBigIntegerToLong((BigInteger)source[0])));
		dbMultsigTransaction.setBlkIndex((Integer)source[9]);
		dbMultsigTransaction.setOrderId((Integer)source[10]);
		dbMultsigTransaction.setReferencedTransaction(castBigIntegerToLong((BigInteger)source[11]));
		dbMultsigTransaction.setTransferTransaction(this.transferSupplier.apply(castBigIntegerToLong((BigInteger)source[12])));
		dbMultsigTransaction.setImportanceTransferTransaction(this.importanceTransferSupplier.apply(castBigIntegerToLong((BigInteger)source[13])));
		dbMultsigTransaction.setMultisigAggregateModificationTransaction(this.multisigModificationTransactionSupplier.apply(castBigIntegerToLong((BigInteger)source[14])));
		dbMultsigTransaction.setMultisigSignatureTransactions(new HashSet<>());

		return dbMultsigTransaction;
	}
}
