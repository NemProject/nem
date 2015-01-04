package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.Transfer;

/**
 * A mapping that is able to map a model transfer transaction to a db transfer.
 */
public class TransferModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<TransferTransaction, Transfer> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public TransferModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public Transfer mapImpl(final TransferTransaction source) {
		final org.nem.nis.dbmodel.Account recipient = this.mapAccount(source.getRecipient());

		final Transfer dbTransfer = new Transfer();
		dbTransfer.setRecipient(recipient);
		dbTransfer.setAmount(source.getAmount().getNumMicroNem());

		dbTransfer.setReferencedTransaction(0L);

		final Message message = source.getMessage();
		if (null != message) {
			dbTransfer.setMessageType(message.getType());
			dbTransfer.setMessagePayload(message.getEncodedPayload());
		}

		return dbTransfer;
	}
}