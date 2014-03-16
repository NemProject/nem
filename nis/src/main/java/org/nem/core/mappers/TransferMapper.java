package org.nem.core.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.dao.AccountDao;
import org.nem.core.dbmodel.*;
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
     * @param accountDao The account data access object.
     * @return The Transfer db-model.
     */
    public static Transfer toDbModel(final TransferTransaction transfer, final int blockIndex, final AccountDao accountDao) {
        final org.nem.core.dbmodel.Account sender = getAccountDbModel(transfer.getSigner(), accountDao);
        final org.nem.core.dbmodel.Account recipient = getAccountDbModel(transfer.getRecipient(), accountDao);

		sender.setPublicKey(transfer.getSigner().getKeyPair().getPublicKey());

        final byte[] txHash = HashUtils.calculateHash(transfer);
        return new Transfer(
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
    }

    private static org.nem.core.dbmodel.Account getAccountDbModel(final Account account, final AccountDao accountDao) {
        return accountDao.getAccountByPrintableAddress(account.getAddress().getEncoded());
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

		TransferTransaction transfer = new TransferTransaction(
            new TimeInstant(dbTransfer.getTimestamp()),
            sender,
            recipient,
            new Amount(dbTransfer.getAmount()),
            null);

        transfer.setFee(new Amount(dbTransfer.getFee()));
        transfer.setDeadline(new TimeInstant(dbTransfer.getDeadline()));
        transfer.setSignature(new Signature(dbTransfer.getSenderProof()));
		return transfer;
	}
}
