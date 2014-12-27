package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db importance transfer to a model importance transfer transaction.
 */
public class ImportanceTransferDbModelToModelMapping implements IMapping<ImportanceTransfer, ImportanceTransferTransaction> {
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
	public ImportanceTransferTransaction map(final ImportanceTransfer source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account remote = this.mapper.map(source.getRemote(), Account.class);

		final ImportanceTransferTransaction transfer = new ImportanceTransferTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				ImportanceTransferTransaction.Mode.fromValueOrDefault(source.getMode()),
				remote);

		transfer.setFee(new Amount(source.getFee()));
		transfer.setDeadline(new TimeInstant(source.getDeadline()));
		transfer.setSignature(new Signature(source.getSenderProof()));
		return transfer;
	}
}
