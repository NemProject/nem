package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

public class MultisigSignatureTransactionMapper {
	static MultisigSignature toDbModel(final MultisigTransaction multisigTransaction, final AccountDaoLookup accountDaoLookup, final MultisigSignatureTransaction model) {
		final MultisigSignature dbModel = new MultisigSignature();

		final Hash txHash = HashUtils.calculateHash(model);
		dbModel.setTransferHash(txHash);
		dbModel.setVersion(model.getVersion());
		dbModel.setFee(model.getFee().getNumMicroNem());
		dbModel.setTimeStamp(model.getTimeStamp().getRawTime());
		dbModel.setDeadline(model.getDeadline().getRawTime());
		dbModel.setSender(accountDaoLookup.findByAddress(model.getSigner().getAddress()));
		dbModel.setSenderProof(model.getSignature().getBytes());

		dbModel.setMultisigTransaction(multisigTransaction);
		return dbModel;
	}

	static MultisigSignatureTransaction toModel(final MultisigSignature multisigSignature, final AccountLookup accountLookup, final org.nem.core.model.MultisigTransaction multisigTransaction) {
		final Address cosignerAddress = AccountToAddressMapper.toAddress(multisigSignature.getSender());
		final Account cosigner = accountLookup.findByAddress(cosignerAddress);

		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(multisigSignature.getTimeStamp()),
				cosigner,
				multisigTransaction.getOtherTransactionHash()
		);
		transaction.setFee(Amount.fromMicroNem(multisigSignature.getFee()));
		transaction.setDeadline(new TimeInstant(multisigSignature.getDeadline()));
		transaction.setSignature(new Signature(multisigSignature.getSenderProof()));
		return transaction;
	}
}
