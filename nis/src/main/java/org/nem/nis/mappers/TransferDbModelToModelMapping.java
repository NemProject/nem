package org.nem.nis.mappers;

import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbTransferTransaction;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a db transfer to a model transfer transaction.
 */
public class TransferDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbTransferTransaction, TransferTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public TransferDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public TransferTransaction mapImpl(final DbTransferTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account recipient = this.mapper.map(source.getRecipient(), Account.class);

		final Message message = messagePayloadToModel(source.getMessagePayload(), source.getMessageType(), sender, recipient);

		final Collection<Mosaic> mosaics = source.getMosaics().stream().map(st -> this.mapper.map(st, Mosaic.class))
				.collect(Collectors.toList());

		final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);
		mosaics.forEach(attachment::addMosaic);
		return new TransferTransaction(source.getVersion() & 0x00FFFFFF, new TimeInstant(source.getTimeStamp()), sender, recipient,
				new Amount(source.getAmount()), attachment);
	}

	private static Message messagePayloadToModel(final byte[] payload, final Integer messageType, final Account sender,
			final Account recipient) {
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
