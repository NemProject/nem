package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbMultisigAggregateModificationTransaction;
import org.nem.nis.mappers.IMapper;

import java.util.HashSet;

/**
 * A mapping that is able to map raw multisig signer modification transaction data to a db multisig signer modification transfer.
 */
public class MultisigAggregateModificationRawToDbModelMapping
		extends
			AbstractTransferRawToDbModelMapping<DbMultisigAggregateModificationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigAggregateModificationRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMultisigAggregateModificationTransaction mapImpl(final Object[] source) {
		final DbMultisigAggregateModificationTransaction dbModificationTransaction = new DbMultisigAggregateModificationTransaction();
		dbModificationTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbModificationTransaction.setBlkIndex((Integer) source[9]);
		dbModificationTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[10]));
		dbModificationTransaction.setMultisigModifications(new HashSet<>());

		return dbModificationTransaction;
	}
}
