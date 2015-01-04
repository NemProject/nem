package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.messages.PlainMessage;
import org.nem.core.messages.SecureMessage;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db transfer to a model transfer transaction.
 */
public class MultisigTransactionDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<org.nem.nis.dbmodel.MultisigTransaction, MultisigTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigTransaction mapImpl(final org.nem.nis.dbmodel.MultisigTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);

		final Transaction otherTransaction;
		if (source.getTransfer() != null) {
			otherTransaction = this.mapper.map(source.getTransfer(), TransferTransaction.class);
		} else if (source.getImportanceTransfer() != null) {
			otherTransaction = this.mapper.map(source.getImportanceTransfer(), ImportanceTransferTransaction.class);
		} else if (source.getMultisigSignerModification() != null) {
			otherTransaction = this.mapper.map(source.getMultisigSignerModification(), MultisigSignerModificationTransaction.class);
		} else {
			throw new RuntimeException("dbmodel has invalid multisig transaction");
		}

		final org.nem.core.model.MultisigTransaction target = new org.nem.core.model.MultisigTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				otherTransaction);


		final MultisigSignatureDbModelToModelMapping multisigSignatureMapper = new MultisigSignatureDbModelToModelMapping(
				this.mapper
		);
		for (final MultisigSignature multisigSignature : source.getMultisigSignatures()) {
			target.addSignature(multisigSignatureMapper.map(multisigSignature));
		}

		return target;
	}
}
