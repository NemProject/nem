package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.MultisigTransaction;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db multisig transfer to a model multisig transaction.
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
		final Transaction otherTransaction = this.mapper.map(getInnerTransaction(source), Transaction.class);

		final org.nem.core.model.MultisigTransaction target = new org.nem.core.model.MultisigTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				otherTransaction);

		for (final DbMultisigSignatureTransaction signature : source.getMultisigSignatureTransactions()) {
			target.addSignature(this.mapper.map(signature, MultisigSignatureTransaction.class));
		}

		return target;
	}

	private static AbstractTransfer getInnerTransaction(final org.nem.nis.dbmodel.MultisigTransaction source) {
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			final AbstractTransfer transaction = entry.getFromMultisig.apply(source);
			if (null != transaction) {
				return transaction;
			}
		}

		throw new IllegalArgumentException("dbmodel has invalid multisig transaction");
	}
}
