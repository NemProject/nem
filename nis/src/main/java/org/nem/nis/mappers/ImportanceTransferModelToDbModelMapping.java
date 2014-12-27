package org.nem.nis.mappers;

import org.nem.core.model.ImportanceTransferTransaction;
import org.nem.nis.dbmodel.ImportanceTransfer;

/**
 * A mapping that is able to map a model importance transfer transaction to a db importance transfer.
 */
public class ImportanceTransferModelToDbModelMapping implements IMapping<ImportanceTransferTransaction, ImportanceTransfer> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ImportanceTransferModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public ImportanceTransfer map(final ImportanceTransferTransaction source) {
		final org.nem.nis.dbmodel.Account sender = this.mapper.map(source.getSigner(), org.nem.nis.dbmodel.Account.class);
		final org.nem.nis.dbmodel.Account remote = this.mapper.map(source.getRemote(), org.nem.nis.dbmodel.Account.class);

		final ImportanceTransfer dbTransfer = new ImportanceTransfer();
		AbstractTransferMapper.toDbModel(source, sender, -1, -1, dbTransfer);

		dbTransfer.setRemote(remote);
		dbTransfer.setMode(source.getMode().value());
		return dbTransfer;
	}
}
