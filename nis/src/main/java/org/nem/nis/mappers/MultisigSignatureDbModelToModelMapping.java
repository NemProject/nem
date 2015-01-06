package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db multisig signature transfer to a model multisig signature.
 */
public class MultisigSignatureDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbMultisigSignatureTransaction, MultisigSignatureTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigSignatureTransaction mapImpl(final DbMultisigSignatureTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		return new MultisigSignatureTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				getInnerTransaction(source.getMultisigTransaction()).getTransferHash());
	}

	// TODO 20150104 J-J: should refactor this somewhere
	private static AbstractTransfer getInnerTransaction(final DbMultisigTransaction source) {
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			final AbstractTransfer transaction = entry.getFromMultisig.apply(source);
			if (null != transaction) {
				return transaction;
			}
		}

		throw new IllegalArgumentException("dbmodel has invalid multisig transaction");
	}
}