package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

/**
 * A mapping that is able to map raw provision namespace transaction data to a db provision namespace transaction.
 */
public class ProvisionNamespaceRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbProvisionNamespaceTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ProvisionNamespaceRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbProvisionNamespaceTransaction mapImpl(final Object[] source) {
		final DbProvisionNamespaceTransaction dbProvisionNamespaceTransaction = new DbProvisionNamespaceTransaction();
		dbProvisionNamespaceTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbProvisionNamespaceTransaction.setBlkIndex((Integer)source[10]);
		dbProvisionNamespaceTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[11]));
		return dbProvisionNamespaceTransaction;
	}
}
