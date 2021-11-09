package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db multisig signature transfer to a model multisig signature.
 */
public class MultisigSignatureDbModelToModelMapping
		extends
			AbstractTransferDbModelToModelMapping<DbMultisigSignatureTransaction, MultisigSignatureTransaction> {
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
		final AbstractTransfer dbOtherTransfer = DbModelUtils.getInnerTransaction(source.getMultisigTransaction());
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account multisig = this.mapper.map(dbOtherTransfer.getSender(), Account.class);
		return new MultisigSignatureTransaction(new TimeInstant(source.getTimeStamp()), sender, multisig,
				dbOtherTransfer.getTransferHash());
	}
}
