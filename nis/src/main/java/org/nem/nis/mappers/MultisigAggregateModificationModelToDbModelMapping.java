package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mapping that is able to map a model multisig signer modification transaction to a db multisig signer modification transfer.
 */
public class MultisigAggregateModificationModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigAggregateModificationModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMultisigAggregateModificationTransaction mapImpl(final MultisigAggregateModificationTransaction source) {
		final DbMultisigAggregateModificationTransaction target = new DbMultisigAggregateModificationTransaction();
		target.setReferencedTransaction(0L);

		final Set<DbMultisigModification> multisigModifications = new HashSet<>(source.getModifications().size());
		for (final MultisigCosignatoryModification multisigCosignatoryModification : source.getModifications()) {
			final DbMultisigModification dbModification = this.mapMultisigModification(multisigCosignatoryModification);
			dbModification.setMultisigAggregateModificationTransaction(target);
			multisigModifications.add(dbModification);
		}

		target.setMultisigModifications(multisigModifications);
		return target;
	}

	private DbMultisigModification mapMultisigModification(final MultisigCosignatoryModification source) {
		final DbAccount cosignatory = this.mapAccount(source.getCosignatory());
		final DbMultisigModification target = new DbMultisigModification();
		target.setCosignatory(cosignatory);
		target.setModificationType(source.getModificationType().value());
		return target;
	}
}