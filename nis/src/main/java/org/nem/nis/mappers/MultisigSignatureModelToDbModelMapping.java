package org.nem.nis.mappers;

import org.nem.core.model.MultisigSignatureTransaction;
import org.nem.nis.dbmodel.DbMultisigSignatureTransaction;

/**
 * A mapping that is able to map a db multisig signature to a model multisig signature.
 */
public class MultisigSignatureModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<MultisigSignatureTransaction, DbMultisigSignatureTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMultisigSignatureTransaction mapImpl(final MultisigSignatureTransaction source) {
		return new DbMultisigSignatureTransaction();
	}
}
