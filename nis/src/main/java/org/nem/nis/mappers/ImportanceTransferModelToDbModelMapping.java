package org.nem.nis.mappers;

import org.nem.core.model.ImportanceTransferTransaction;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model importance transfer transaction to a db importance transfer.
 */
public class ImportanceTransferModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<ImportanceTransferTransaction, DbImportanceTransferTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ImportanceTransferModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbImportanceTransferTransaction mapImpl(final ImportanceTransferTransaction source) {
		final DbAccount remote = this.mapAccount(source.getRemote());

		final DbImportanceTransferTransaction dbTransfer = new DbImportanceTransferTransaction();
		dbTransfer.setRemote(remote);
		dbTransfer.setMode(source.getMode().value());
		dbTransfer.setReferencedTransaction(0L);
		return dbTransfer;
	}
}
