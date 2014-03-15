package org.nem.core.mappers;

import org.nem.core.dao.AccountDao;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
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
}
