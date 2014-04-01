package org.nem.core.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.dbmodel.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;

import java.security.InvalidParameterException;

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
     * @param accountDaoLookup The account dao lookup object.
     * @return The Transfer db-model.
     */
    public static Transfer toDbModel(final TransferTransaction transfer, final int blockIndex, final AccountDaoLookup accountDaoLookup) {
        final org.nem.core.dbmodel.Account sender = accountDaoLookup.findByAddress(transfer.getSigner().getAddress());
        final org.nem.core.dbmodel.Account recipient = accountDaoLookup.findByAddress(transfer.getRecipient().getAddress());

        final byte[] txHash = HashUtils.calculateHash(transfer);
        final Transfer dbTransfer = new Transfer(
            ByteUtils.bytesToLong(txHash),
            txHash,
            transfer.getVersion(),
            transfer.getType(),
            transfer.getFee().getNumMicroNem(),
            transfer.getTimeStamp().getRawTime(),
            transfer.getDeadline().getRawTime(),
            sender,
            // proof
            transfer.getSignature().getBytes(),
            recipient,
            blockIndex, // index
            transfer.getAmount().getNumMicroNem(),
            0L); // referenced tx

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
	 *
	 * @return The TransferTransaction model.
	 */
	public static TransferTransaction toModel(final Transfer dbTransfer, final AccountLookup accountLookup) {
        final Address senderAccount = Address.fromPublicKey(dbTransfer.getSender().getPublicKey());
		final Account sender = accountLookup.findByAddress(senderAccount);

        final Address recipientAccount = Address.fromEncoded(dbTransfer.getRecipient().getPrintableKey());
		final Account recipient = accountLookup.findByAddress(recipientAccount);

		final Message message = messagePayloadToModel(
            dbTransfer.getMessagePayload(),
            dbTransfer.getMessageType(),
            sender,
            recipient);

		final TransferTransaction transfer = new TransferTransaction(
            new TimeInstant(dbTransfer.getTimestamp()),
            sender,
            recipient,
            new Amount(dbTransfer.getAmount()),
            message);

        transfer.setFee(new Amount(dbTransfer.getFee()));
        transfer.setDeadline(new TimeInstant(dbTransfer.getDeadline()));
        transfer.setSignature(new Signature(dbTransfer.getSenderProof()));
		return transfer;
	}

    private static Message messagePayloadToModel(final byte[] payload, final Integer messageType, final Account sender, final Account recipient) {
        if (null == payload)
            return null;

        switch (messageType) {
            case MessageTypes.PLAIN:
                return new PlainMessage(payload);

            case MessageTypes.SECURE:
                return SecureMessage.fromEncodedPayload(sender, recipient, payload);
        }

        throw new InvalidParameterException("Unknown message type in database");
    }
}
