package org.nem.core.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.dbmodel.*;
import org.nem.core.messages.MessageFactory;
import org.nem.core.messages.PlainMessage;
import org.nem.core.messages.SecureMessage;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;

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
        Transfer dbTransfer = new Transfer(
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

		if (transfer.getMessage() != null) {
			dbTransfer.setMessageType(transfer.getMessage().getType());
			dbTransfer.setMessagePayload(transfer.getMessage().getEncodedPayload());
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

		Message message = null;
		if (dbTransfer.getMessagePayload() != null) {
			switch (dbTransfer.getMessageType()) {
				case MessageTypes.PLAIN:
					message = new PlainMessage(dbTransfer.getMessagePayload());
					break;
				case MessageTypes.SECURE:
					message = new SecureMessage(sender, recipient, dbTransfer.getMessagePayload());
					break;
			}
		}

		TransferTransaction transfer = new TransferTransaction(
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
}
