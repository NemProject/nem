package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db multisig signature to a model multisig signature.
 */
public class MultisigSignatureModelToDbModelMapping implements IMapping<MultisigSignatureTransaction, MultisigSignature> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigSignature map(final MultisigSignatureTransaction source) {
		final MultisigSignature dbSignature = new MultisigSignature();
		final org.nem.nis.dbmodel.Account sender = this.mapper.map(source.getSigner(), org.nem.nis.dbmodel.Account.class);

		final Hash txHash = HashUtils.calculateHash(source);
		dbSignature.setTransferHash(txHash);
		dbSignature.setVersion(source.getVersion());
		dbSignature.setFee(source.getFee().getNumMicroNem());
		dbSignature.setTimeStamp(source.getTimeStamp().getRawTime());
		dbSignature.setDeadline(source.getDeadline().getRawTime());
		dbSignature.setSender(sender);
		dbSignature.setSenderProof(source.getSignature().getBytes());

		// TODO 20141203 J-G: why do we need this?
		dbSignature.setMultisigTransaction(null);
		return dbSignature;
	}
}
