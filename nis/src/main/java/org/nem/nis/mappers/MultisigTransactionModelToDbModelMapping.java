package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.MultisigTransaction;
import org.nem.nis.dbmodel.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a multisig transaction to a db multisig transaction.
 */
public class MultisigTransactionModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigTransaction, org.nem.nis.dbmodel.MultisigTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public org.nem.nis.dbmodel.MultisigTransaction mapImpl(final MultisigTransaction source) {
		final org.nem.nis.dbmodel.MultisigTransaction dbMultisigTransfer = new org.nem.nis.dbmodel.MultisigTransaction();
		dbMultisigTransfer.setReferencedTransaction(0L);

		// TODO 20150104 J-J: move to registry (hopefully)
		final Transaction transaction = source.getOtherTransaction();
		switch (source.getOtherTransaction().getType()) {
			case TransactionTypes.TRANSFER:
				dbMultisigTransfer.setTransfer(this.mapper.map(transaction, Transfer.class));
				break;

			case TransactionTypes.IMPORTANCE_TRANSFER:
				dbMultisigTransfer.setImportanceTransfer(this.mapper.map(transaction, ImportanceTransfer.class));
				break;

			case TransactionTypes.MULTISIG_SIGNER_MODIFY:
				dbMultisigTransfer.setMultisigSignerModification(this.mapper.map(transaction, MultisigSignerModification.class));
				break;

			default:
				throw new IllegalArgumentException("trying to map block with unknown transaction type");
		}

		final Set<MultisigSignature> multisigSignatures = source.getCosignerSignatures().stream()
				.map(model -> this.mapper.map(model, MultisigSignature.class))
				.collect(Collectors.toSet());

		dbMultisigTransfer.setMultisigSignatures(multisigSignatures);
		return dbMultisigTransfer;
	}
}