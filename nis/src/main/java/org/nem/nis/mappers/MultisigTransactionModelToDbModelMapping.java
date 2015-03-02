package org.nem.nis.mappers;

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
		// TODO 20150302 BR -> J: I gave it a try, not sure if it is good enough.
		final Transaction transaction = source.getOtherTransaction();
		boolean success = false;
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			if (entry.modelClass.equals(transaction.getClass())) {
				final AbstractBlockTransfer inner = this.mapper.map(transaction, entry.dbModelClass);
				success = entry.setInMultisig.apply(dbMultisigTransfer, inner);
				break;
			}
		}

		if (!success) {
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