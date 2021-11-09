package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.util.Arrays;

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
		final DbAccount dbRentalFeeSink = RawMapperUtils.mapAccount(this.mapper, source[9]);
		final DbNamespace dbNamespace = this.mapper.map(Arrays.copyOfRange(source, 14, source.length), DbNamespace.class);
		final DbProvisionNamespaceTransaction dbProvisionNamespaceTransaction = new DbProvisionNamespaceTransaction();
		dbProvisionNamespaceTransaction.setNamespace(dbNamespace);
		dbProvisionNamespaceTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbProvisionNamespaceTransaction.setRentalFeeSink(dbRentalFeeSink);
		dbProvisionNamespaceTransaction.setRentalFee(RawMapperUtils.castToLong(source[10]));
		dbProvisionNamespaceTransaction.setBlkIndex((Integer) source[12]);
		dbProvisionNamespaceTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[13]));
		return dbProvisionNamespaceTransaction;
	}
}
