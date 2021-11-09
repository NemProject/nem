package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

/**
 * A mapping that is able to map raw importance transfer transaction data to a db importance transfer.
 */
public class ImportanceTransferRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbImportanceTransferTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ImportanceTransferRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbImportanceTransferTransaction mapImpl(final Object[] source) {
		final DbAccount remote = RawMapperUtils.mapAccount(this.mapper, source[9]);

		final DbImportanceTransferTransaction dbImportanceTransfer = new DbImportanceTransferTransaction();
		dbImportanceTransfer.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbImportanceTransfer.setRemote(remote);
		dbImportanceTransfer.setMode((Integer) source[10]);
		dbImportanceTransfer.setBlkIndex((Integer) source[11]);
		dbImportanceTransfer.setReferencedTransaction(RawMapperUtils.castToLong(source[12]));

		return dbImportanceTransfer;
	}
}
