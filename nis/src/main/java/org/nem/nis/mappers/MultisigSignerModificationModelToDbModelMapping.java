package org.nem.nis.mappers;

import org.nem.core.model.MultisigSignerModificationTransaction;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mapping that is able to map a model multisig signer modification transaction to a db multisig signer modification transfer.
 */
public class MultisigSignerModificationModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigSignerModificationTransaction, MultisigSignerModification> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignerModificationModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public MultisigSignerModification mapImpl(final MultisigSignerModificationTransaction source) {
		final MultisigSignerModification target = new MultisigSignerModification();
		target.setReferencedTransaction(0L);

		final Set<MultisigModification> multisigModifications = new HashSet<>(source.getModifications().size());
		for (final org.nem.core.model.MultisigModification multisigModification : source.getModifications()) {
			final MultisigModification dbModification = this.mapMultisigModification(multisigModification);
			dbModification.setMultisigSignerModification(target);
			multisigModifications.add(dbModification);
		}

		target.setMultisigModifications(multisigModifications);
		return target;
	}

	private MultisigModification mapMultisigModification(final org.nem.core.model.MultisigModification source) {
		final org.nem.nis.dbmodel.Account cosignatory = this.mapAccount(source.getCosignatory());
		final org.nem.nis.dbmodel.MultisigModification target = new org.nem.nis.dbmodel.MultisigModification();
		target.setCosignatory(cosignatory);
		target.setModificationType(source.getModificationType().value());
		return target;
	}
}