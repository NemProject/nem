package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigTransaction;

public class MultisigTransactionMapper {

	public static MultisigTransaction toDbModel(
			final org.nem.core.model.MultisigTransaction transaction,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(transaction.getSigner().getAddress());

		final MultisigTransaction transfer = new MultisigTransaction();
		AbstractTransferMapper.toDbModel(transaction, sender, blockIndex, orderIndex, transfer);

		return transfer;
	}

	public static org.nem.core.model.MultisigTransaction toModel(final MultisigTransaction dbTransfer, final AccountLookup accountLookup) {
		Transaction otherTransaction;
		if (dbTransfer.getTransfer() != null) {
			otherTransaction = TransferMapper.toModel(dbTransfer.getTransfer(), accountLookup);
		} else if (dbTransfer.getImportanceTransfer() != null) {
			otherTransaction = ImportanceTransferMapper.toModel(dbTransfer.getImportanceTransfer(), accountLookup);
		} else if (dbTransfer.getMultisigSignerModification() != null) {
			otherTransaction = MultisigSignerModificationMapper.toModel(dbTransfer.getMultisigSignerModification(), accountLookup);
		} else {
			throw new RuntimeException("dbmodel has invalid multisig transaction");
		}

		final Address senderAccount = AccountToAddressMapper.toAddress(dbTransfer.getSender());
		final Account sender = accountLookup.findByAddress(senderAccount);

		final org.nem.core.model.MultisigTransaction transaction = new org.nem.core.model.MultisigTransaction(
				new TimeInstant(dbTransfer.getTimeStamp()),
				sender,
				otherTransaction);

		transaction.setFee(new Amount(dbTransfer.getFee()));
		transaction.setDeadline(new TimeInstant(dbTransfer.getDeadline()));
		transaction.setSignature(new Signature(dbTransfer.getSenderProof()));

		return transaction;
	}
}
