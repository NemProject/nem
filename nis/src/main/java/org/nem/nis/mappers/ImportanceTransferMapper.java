package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class ImportanceTransferMapper {
	/**
	 * Converts a ImportanceTransferTransaction model to a ImportanceTransfer db-model.
	 *
	 * @param importanceTransferTransaction The transfer transaction model.
	 * @param blockIndex The index of the transfer within the owning block.
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The ImportanceTransfer db-model.
	 */
	public static ImportanceTransfer toDbModel(final ImportanceTransferTransaction importanceTransferTransaction, final int blockIndex, final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(importanceTransferTransaction.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account remote = accountDaoLookup.findByAddress(importanceTransferTransaction.getRemote().getAddress());

		final Hash txHash = HashUtils.calculateHash(importanceTransferTransaction);
		final ImportanceTransfer dbTransfer = new ImportanceTransfer(
				txHash,
				importanceTransferTransaction.getVersion(),
				importanceTransferTransaction.getType(),
				importanceTransferTransaction.getFee().getNumMicroNem(),
				importanceTransferTransaction.getTimeStamp().getRawTime(),
				importanceTransferTransaction.getDeadline().getRawTime(),
				sender,
				// proof
				importanceTransferTransaction.getSignature().getBytes(),
				remote,
				importanceTransferTransaction.getDirection(),
				blockIndex, // index
				0L); // referenced tx

		return dbTransfer;
	}

	/**
	 * Converts a ImportanceTransfer db-model to a ImportanceTransferTransaction model.
	 *
	 * @param dbImportanceTransfer The importance transfer db-model.
	 * @param accountLookup The account lookup object.
	 * @return The ImportanceTransferTransaction model.
	 */
	public static ImportanceTransferTransaction toModel(final ImportanceTransfer dbImportanceTransfer, final AccountLookup accountLookup) {
		final Address senderAccount = Address.fromPublicKey(dbImportanceTransfer.getSender().getPublicKey());
		final Account sender = accountLookup.findByAddress(senderAccount);

		final Address remoteAddress = Address.fromPublicKey(dbImportanceTransfer.getRemote().getPublicKey());
		final Account remote = accountLookup.findByAddress(remoteAddress);

		final ImportanceTransferTransaction transfer = new ImportanceTransferTransaction(
				new TimeInstant(dbImportanceTransfer.getTimeStamp()),
				sender,
				dbImportanceTransfer.getDirection(),
				remote);

		transfer.setFee(new Amount(dbImportanceTransfer.getFee()));
		transfer.setDeadline(new TimeInstant(dbImportanceTransfer.getDeadline()));
		transfer.setSignature(new Signature(dbImportanceTransfer.getSenderProof()));
		return transfer;
	}

}
