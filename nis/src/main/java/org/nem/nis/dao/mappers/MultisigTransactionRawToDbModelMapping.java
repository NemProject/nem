package org.nem.nis.dao.mappers;

import org.nem.core.model.TransactionTypes;
import org.nem.nis.dao.MultisigTransferMap;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.util.HashSet;

/**
 * A mapping that is able to map raw multisig transaction data to a db multisig transaction.
 */
@SuppressWarnings("rawtypes")
public class MultisigTransactionRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMultisigTransaction> {
	private final MultisigTransferMap multisigTransferMap;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 * @param multisigTransferMap Map containing id to transfer mappings for (inner) multisig transactions.
	 */
	public MultisigTransactionRawToDbModelMapping(final IMapper mapper, final MultisigTransferMap multisigTransferMap) {
		super(mapper);
		this.multisigTransferMap = multisigTransferMap;
	}

	@Override
	protected DbMultisigTransaction mapImpl(final Object[] source) {
		final DbMultisigTransaction dbMultisigTransaction = new DbMultisigTransaction();
		dbMultisigTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbMultisigTransaction.setBlkIndex((Integer) source[9]);
		dbMultisigTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[10]));

		int offset = 0;
		for (final int type : TransactionTypes.getMultisigEmbeddableTypes()) {
			final TransactionRegistry.Entry registryEntry = TransactionRegistry.findByType(type);
			assert null != registryEntry;

			final MultisigTransferMap.Entry transferMapEntry = this.multisigTransferMap.getEntry(type);
			final Long id = RawMapperUtils.castToLong(source[11 + offset++]);
			set(registryEntry, dbMultisigTransaction, transferMapEntry.getOrDefault(id));
		}

		dbMultisigTransaction.setMultisigSignatureTransactions(new HashSet<>());
		return dbMultisigTransaction;
	}

	@SuppressWarnings("unchecked")
	private static void set(final TransactionRegistry.Entry registryEntry, final DbMultisigTransaction dbMultisigTransaction,
			final AbstractBlockTransfer dbTransfer) {
		registryEntry.setInMultisig.accept(dbMultisigTransaction, dbTransfer);
	}
}
