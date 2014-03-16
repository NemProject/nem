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
import org.nem.nis.AccountAnalyzer;

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

	/**
	 * Converts Transfer db-model to a TransferTransaction model.
	 *
	 * @param transfer db-model transfer orm object.
	 * @param accountAnalyzer analyzer containing information about accounts.
	 *
	 * @return TransterTransaction model.
	 */
	public static TransferTransaction toModel(final Transfer transfer, AccountLookup accountAnalyzer) {
		org.nem.core.model.Account sender = accountAnalyzer.findByAddress(Address.fromPublicKey(transfer.getSender().getPublicKey()));
		org.nem.core.model.Account recipient = accountAnalyzer.findByAddress(Address.fromEncoded(transfer.getRecipient().getPrintableKey()));
		TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(transfer.getTimestamp()),
				sender,
				recipient,
				new Amount(transfer.getAmount()),
				null
		);
		transferTransaction.setFee(new Amount(transfer.getFee()));
		transferTransaction.setDeadline(new TimeInstant(transfer.getDeadline()));
		transferTransaction.setSignature(new Signature(transfer.getSenderProof()));
		return transferTransaction;
	}
}
