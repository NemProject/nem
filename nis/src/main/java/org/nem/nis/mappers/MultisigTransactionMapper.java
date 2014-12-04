package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignature;
import org.nem.nis.dbmodel.MultisigTransaction;

import java.util.HashSet;
import java.util.Set;

// TODO 20141201 J-J: i will need to look at this a bit closer
// TODO 20141201 J-J: guess we need tests for this
// TODO 20141202 G-J: since it's heavily intertwined with BlockMapper, I wasn't sure
// how to make some sensible tests, but I've added few tests in BlockMapperTest

public class MultisigTransactionMapper {

	public static MultisigTransaction toDbModel(
			final org.nem.core.model.MultisigTransaction transaction,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(transaction.getSigner().getAddress());

		final MultisigTransaction multisigTransaction = new MultisigTransaction();
		AbstractTransferMapper.toDbModel(transaction, sender, blockIndex, orderIndex, multisigTransaction);

		// begin MultisigSignature.toDbModel()
		// extract to separate mapper
		final Set<MultisigSignature> multisigSignatures = new HashSet<>();
		for (final MultisigSignatureTransaction model : transaction.getCosignerSignatures()) {
			final MultisigSignature dbModel = new MultisigSignature();

			final Hash txHash = HashUtils.calculateHash(model);
			dbModel.setTransferHash(txHash);
			dbModel.setVersion(model.getVersion());
			dbModel.setFee(model.getFee().getNumMicroNem());
			dbModel.setTimeStamp(model.getTimeStamp().getRawTime());
			dbModel.setDeadline(model.getDeadline().getRawTime());
			dbModel.setSender(sender);
			dbModel.setSenderProof(model.getSignature().getBytes());

			dbModel.setMultisigTransaction(multisigTransaction);

			multisigSignatures.add(dbModel);
		}
		multisigTransaction.setMultisigSignatures(multisigSignatures);
		// end MultisigSignature.toDbModel()

		// proper multisigTransaction.set*Transfer(); are called from BlockMapper

		return multisigTransaction;
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

		// begin MultisigSignature.toModel()
		for (final MultisigSignature multisigSignature : dbTransfer.getMultisigSignatures()) {
			final Address cosignerAddress = AccountToAddressMapper.toAddress(multisigSignature.getSender());
			final Account cosigner = accountLookup.findByAddress(cosignerAddress);

			transaction.addSignature(
					new MultisigSignatureTransaction(
							new TimeInstant(dbTransfer.getTimeStamp()),
							cosigner,
							HashUtils.calculateHash(otherTransaction),
							new Signature(new byte[64])
					)
			);
		}
		// end MultisigSignature.toModel()

		return transaction;
	}
}
