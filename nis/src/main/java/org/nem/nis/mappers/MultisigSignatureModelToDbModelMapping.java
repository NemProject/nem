package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db multisig signature to a model multisig signature.
 */
public class MultisigSignatureModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigSignatureTransaction, MultisigSignature> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected MultisigSignature mapImpl(final MultisigSignatureTransaction source) {
		return new MultisigSignature();
	}
}
