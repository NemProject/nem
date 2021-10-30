package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a multisig transaction to a db multisig transaction.
 */
public class MultisigTransactionModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<MultisigTransaction, DbMultisigTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public DbMultisigTransaction mapImpl(final MultisigTransaction source) {
		final DbMultisigTransaction dbMultisigTransfer = new DbMultisigTransaction();
		dbMultisigTransfer.setReferencedTransaction(0L);

		final Transaction transaction = source.getOtherTransaction();
		final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(transaction.getType());
		if (null == entry || null == entry.setInMultisig) {
			throw new IllegalArgumentException("trying to map block with unknown transaction type");
		}

		final AbstractBlockTransfer inner = this.mapper.map(transaction, entry.dbModelClass);
		entry.setInMultisig.accept(dbMultisigTransfer, inner);

		final Set<DbMultisigSignatureTransaction> multisigSignatureTransactions = source.getCosignerSignatures().stream().map(model -> {
			final DbMultisigSignatureTransaction signature = this.mapper.map(model, DbMultisigSignatureTransaction.class);
			signature.setMultisigTransaction(dbMultisigTransfer);
			return signature;
		}).collect(Collectors.toSet());

		dbMultisigTransfer.setMultisigSignatureTransactions(multisigSignatureTransactions);
		return dbMultisigTransfer;
	}
}
