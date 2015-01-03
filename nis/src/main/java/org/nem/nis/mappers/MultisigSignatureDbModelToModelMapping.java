package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignature;

/**
 * A mapping that is able to map a db multisig signature transfer to a model multisig signature.
 */
public class MultisigSignatureDbModelToModelMapping implements IMapping<MultisigSignature, MultisigSignatureTransaction> {
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
	public MultisigSignatureTransaction map(final MultisigSignature source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);

		final MultisigSignatureTransaction transfer = new MultisigSignatureTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				source.getMultisigTransaction().getTransferHash()); // TODO 20140103: this is probably wrong

		transfer.setFee(new Amount(source.getFee()));
		transfer.setDeadline(new TimeInstant(source.getDeadline()));
		transfer.setSignature(new Signature(source.getSenderProof()));
		return transfer;
	}
}