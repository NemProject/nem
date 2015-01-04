package org.nem.nis.mappers;

import org.nem.core.model.Message;
import org.nem.core.model.MultisigSignerModificationTransaction;
import org.nem.core.model.TransferTransaction;
import org.nem.nis.dbmodel.MultisigModification;
import org.nem.nis.dbmodel.MultisigSignerModification;
import org.nem.nis.dbmodel.Transfer;

import java.util.HashSet;
import java.util.Set;

/**
 * A mapping that is able to map a model transfer transaction to a db transfer.
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