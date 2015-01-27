package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbMultisigSignatureTransaction;

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
		final Account multisig = this.mapper.map(source.getMultisigTransaction().getSender(), Account.class);
		return new MultisigSignatureTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				// TODO 20140126 J-J: need to validate this is set correctly
				multisig,
				DbModelUtils.getInnerTransaction(source.getMultisigTransaction()).getTransferHash());
	}
}