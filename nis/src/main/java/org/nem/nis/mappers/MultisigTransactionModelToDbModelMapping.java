package org.nem.nis.mappers;

import org.nem.core.model.Message;
import org.nem.core.model.MultisigSignatureTransaction;
import org.nem.core.model.TransferTransaction;
import org.nem.nis.dbmodel.MultisigSignature;
import org.nem.nis.dbmodel.MultisigTransaction;
import org.nem.nis.dbmodel.Transfer;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a multisig transaction to a db multisig transaction.
 */
public class MultisigTransactionModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<org.nem.core.model.MultisigTransaction, MultisigTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigTransactionModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public MultisigTransaction mapImpl(final org.nem.core.model.MultisigTransaction source) {
		final MultisigTransaction multisigTransaction = new MultisigTransaction();
		multisigTransaction.setReferencedTransaction(0L);

		final Set<MultisigSignature> multisigSignatures = source.getCosignerSignatures().stream()
				.map(model -> this.mapper.map(model, MultisigSignature.class))
				.collect(Collectors.toSet());
		multisigTransaction.setMultisigSignatures(multisigSignatures);
		return multisigTransaction;
	}
}