package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignature;

public class MultisigSignatureTransactionMapper {
	static MultisigSignature toDbModel(org.nem.nis.dbmodel.Account sender, MultisigSignatureTransaction model) {
		final MultisigSignature dbModel = new MultisigSignature();

		final Hash txHash = HashUtils.calculateHash(model);
		dbModel.setTransferHash(txHash);
		dbModel.setVersion(model.getVersion());
		dbModel.setFee(model.getFee().getNumMicroNem());
		dbModel.setTimeStamp(model.getTimeStamp().getRawTime());
		dbModel.setDeadline(model.getDeadline().getRawTime());
		dbModel.setSender(sender);
		dbModel.setSenderProof(model.getSignature().getBytes());
		return dbModel;
	}

	static MultisigSignatureTransaction toModel(final MultisigSignature multisigSignature, final AccountLookup accountLookup, final Transaction otherTransaction) {
		final Address cosignerAddress = AccountToAddressMapper.toAddress(multisigSignature.getSender());
		final Account cosigner = accountLookup.findByAddress(cosignerAddress);

		return new MultisigSignatureTransaction(
				new TimeInstant(multisigSignature.getTimeStamp()),
				cosigner,
				HashUtils.calculateHash(otherTransaction)
		);
	}
}
