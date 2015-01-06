package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.ImportanceTransfer;

/**
 * A mapping that is able to map a db importance transfer to a model importance transfer transaction.
 */
public class ImportanceTransferDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<ImportanceTransfer, ImportanceTransferTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ImportanceTransferDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public ImportanceTransferTransaction mapImpl(final ImportanceTransfer source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account remote = this.mapper.map(source.getRemote(), Account.class);

		return new ImportanceTransferTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				ImportanceTransferTransaction.Mode.fromValueOrDefault(source.getMode()),
				remote);
	}
}
