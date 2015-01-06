package org.nem.nis.mappers;

import org.nem.core.model.MultisigTransaction;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a multisig transaction to a db multisig transaction.
 */
public class MultisigTransactionModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigTransaction, DbMultisigTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMultisigTransaction mapImpl(final MultisigTransaction source) {
		final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
		dbMultisigTransfer.setReferencedTransaction(0L);

		// TODO 20150104 J-J: move to registry (hopefully)
		final Transaction transaction = source.getOtherTransaction();
		switch (source.getOtherTransaction().getType()) {
			case TransactionTypes.TRANSFER:
				dbMultisigTransfer.setTransferTransaction(this.mapper.map(transaction, DbTransferTransaction.class));
				break;

			case TransactionTypes.IMPORTANCE_TRANSFER:
				dbMultisigTransfer.setImportanceTransferTransaction(this.mapper.map(transaction, DbImportanceTransferTransaction.class));
				break;

			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				dbMultisigTransfer.setMultisigAggregateModificationTransaction(this.mapper.map(transaction, DbMultisigAggregateModificationTransaction.class));
				break;

			default:
				throw new IllegalArgumentException("trying to map block with unknown transaction type");
		}

		final Set<DbMultisigSignatureTransaction> multisigSignatureTransactions = source.getCosignerSignatures().stream()
				.map(model -> {
					final DbMultisigSignatureTransaction signature = this.mapper.map(model, DbMultisigSignatureTransaction.class);
					signature.setMultisigTransaction(dbMultisigTransfer);
					return signature;
				})
				.collect(Collectors.toSet());

		dbMultisigTransfer.setMultisigSignatureTransactions(multisigSignatureTransactions);
		return dbMultisigTransfer;
	}
}