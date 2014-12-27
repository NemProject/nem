package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.Transfer;

/**
 * A mapping that is able to map a model transfer transaction to a db transfer.
 */
public class TransferModelToDbModelMapping implements IMapping<TransferTransaction, Transfer> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public TransferModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Transfer map(final TransferTransaction source) {
		final org.nem.nis.dbmodel.Account sender = this.mapper.map(source.getSigner(), org.nem.nis.dbmodel.Account.class);
		final org.nem.nis.dbmodel.Account recipient = this.mapper.map(source.getRecipient(), org.nem.nis.dbmodel.Account.class);

		final Transfer dbTransfer = new Transfer();
		AbstractTransferMapper.toDbModel(source, sender, -1, -1, dbTransfer);

		dbTransfer.setRecipient(recipient);
		dbTransfer.setAmount(source.getAmount().getNumMicroNem());

		final Message message = source.getMessage();
		if (null != message) {
			dbTransfer.setMessageType(message.getType());
			dbTransfer.setMessagePayload(message.getEncodedPayload());
		}

		return dbTransfer;
	}
}