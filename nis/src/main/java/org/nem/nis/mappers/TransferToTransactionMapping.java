package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;
/**
 * A mapping that is able to map a db transfer to a model transfer transaction.
 */
public class TransferToTransactionMapping implements IMapping<Transfer, TransferTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper the mapper.
	 */
	public TransferToTransactionMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public TransferTransaction map(final Transfer source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account recipient = this.mapper.map(source.getRecipient(), Account.class);

		final Message message = messagePayloadToModel(
				source.getMessagePayload(),
				source.getMessageType(),
				sender,
				recipient);

		final TransferTransaction transfer = new TransferTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				recipient,
				new Amount(source.getAmount()),
				message);

		transfer.setFee(new Amount(source.getFee()));
		transfer.setDeadline(new TimeInstant(source.getDeadline()));
		transfer.setSignature(new Signature(source.getSenderProof()));
		return transfer;
	}

	private static Message messagePayloadToModel(final byte[] payload, final Integer messageType, final Account sender, final Account recipient) {
		if (null == payload) {
			return null;
		}

		switch (messageType) {
			case MessageTypes.PLAIN:
				return new PlainMessage(payload);

			case MessageTypes.SECURE:
				return SecureMessage.fromEncodedPayload(sender, recipient, payload);
		}

		throw new IllegalArgumentException("Unknown message type in database");
	}
}
