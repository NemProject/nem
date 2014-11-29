package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

// TODO 20141119 - this is a temporary class to illustrate the MappedSuperclass
public class AbstractTransferMapper {

	public static void toDbModel(
			final Transaction model,
			final org.nem.nis.dbmodel.Account sender,
			final int blockIndex,
			final int orderIndex,
			final AbstractTransfer dbModel) {
		final Hash txHash = HashUtils.calculateHash(model);
		dbModel.setTransferHash(txHash);
		dbModel.setVersion(model.getVersion());
		dbModel.setFee(model.getFee().getNumMicroNem());
		dbModel.setTimeStamp(model.getTimeStamp().getRawTime());
		dbModel.setDeadline(model.getDeadline().getRawTime());
		dbModel.setSender(sender);
		dbModel.setSenderProof(model.getSignature() == null ? null : model.getSignature().getBytes());
		dbModel.setOrderId(orderIndex); // TODO 20141119 J-G: not sure the point of this since you are never actually returning this from getOrderId??
		dbModel.setBlkIndex(blockIndex);
		dbModel.setReferencedTransaction(0L);
	}
}
