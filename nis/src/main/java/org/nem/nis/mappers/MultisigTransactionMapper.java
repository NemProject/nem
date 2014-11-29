package org.nem.nis.mappers;

import org.nem.core.model.ImportanceTransferTransaction;
import org.nem.core.model.MultisigSignerModificationTransaction;
import org.nem.core.model.TransactionTypes;
import org.nem.core.model.TransferTransaction;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.dbmodel.MultisigSignerModification;
import org.nem.nis.dbmodel.MultisigTransaction;
import org.nem.nis.dbmodel.Transfer;

public class MultisigTransactionMapper {

	public static MultisigTransaction toDbModel(
			final org.nem.core.model.MultisigTransaction transaction,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(transaction.getSigner().getAddress());

		final MultisigTransaction transfer = new MultisigTransaction();
		AbstractTransferMapper.toDbModel(transaction, sender, blockIndex, orderIndex, transfer);

		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER: {

			}
			break;
			case TransactionTypes.IMPORTANCE_TRANSFER: {

			}
			break;
			case TransactionTypes.MULTISIG_SIGNER_MODIFY: {

			}
			break;
			case TransactionTypes.MULTISIG: {

			}
			break;
			default:
				throw new RuntimeException("trying to map block with unknown transaction type");
		}

		//transfer.setCosignatory(remote);
		//transfer.setModificationType(multisigSignerModification.getModificationType().value());

		return transfer;
	}
}
