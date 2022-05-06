package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbMultisigSignatureTransaction;
import org.nem.nis.mappers.IMapper;

/**
 * A mapping that is able to map raw multisig signature transaction data to a db multisig signature.
 */
public class MultisigSignatureRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMultisigSignatureTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMultisigSignatureTransaction mapImpl(final Object[] source) {
		return new DbMultisigSignatureTransaction();
	}
}
