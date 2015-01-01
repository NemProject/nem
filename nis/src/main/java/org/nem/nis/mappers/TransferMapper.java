package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;

/**
 * Static class that contains functions for converting to and from
 * db-model Transfer and model TransferTransaction.
 */
public class TransferMapper {

	/**
	 * Converts a TransferTransaction model to a Transfer db-model.
	 *
	 * @param transfer The transfer transaction model.
	 * @param blockIndex The index of the transfer within the owning block.
	 * @param orderIndex The index of the transfer within the owning block's collection of similar transactions.
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The Transfer db-model.
	 */
	public static Transfer toDbModel(
			final TransferTransaction transfer,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(transfer.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account recipient = accountDaoLookup.findByAddress(transfer.getRecipient().getAddress());

		final Transfer dbTransfer = new Transfer();
		AbstractTransferMapper.toDbModel(transfer, sender, blockIndex, orderIndex, dbTransfer);

		dbTransfer.setRecipient(recipient);
		dbTransfer.setAmount(transfer.getAmount().getNumMicroNem());

		final Message message = transfer.getMessage();
		if (null != message) {
			dbTransfer.setMessageType(message.getType());
			dbTransfer.setMessagePayload(message.getEncodedPayload());
		}

		return dbTransfer;
	}

	/**
	 * Converts a Transfer db-model to a TransferTransaction model.
	 *
	 * @param dbTransfer The transfer db-model.
	 * @param accountLookup The account lookup object.
	 * @return The TransferTransaction model.
	 */
	public static TransferTransaction toModel(final Transfer dbTransfer, final AccountLookup accountLookup) {
		final Address senderAccount = AccountToAddressMapper.toAddress(dbTransfer.getSender());
		final Account sender = accountLookup.findByAddress(senderAccount);

		final Address recipientAccount = Address.fromEncoded(dbTransfer.getRecipient().getPrintableKey());
		final Account recipient = accountLookup.findByAddress(recipientAccount);

		final Message message = messagePayloadToModel(
				dbTransfer.getMessagePayload(),
				dbTransfer.getMessageType(),
				sender,
				recipient);

		final TransferTransaction transfer = new TransferTransaction(
				new TimeInstant(dbTransfer.getTimeStamp()),
				sender,
				recipient,
				new Amount(dbTransfer.getAmount()),
				message);

		transfer.setFee(new Amount(dbTransfer.getFee()));
		transfer.setDeadline(new TimeInstant(dbTransfer.getDeadline()));
		// TODO 20141201 J-G: when do we expect the signature to be null?
		// TODO 20141202 G-J: When TransferTransaction (unsigned ofc) is inside MultisigTransaction?
		transfer.setSignature(dbTransfer.getSenderProof() == null ? null : new Signature(dbTransfer.getSenderProof()));
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
