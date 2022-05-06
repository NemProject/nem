package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mapping that is able to map a model multisig signer modification transaction to a db multisig signer modification transfer.
 */
public class MultisigAggregateModificationModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> {

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

		final Set<DbMultisigModification> multisigModifications = new HashSet<>(source.getCosignatoryModifications().size());
		for (final MultisigCosignatoryModification multisigCosignatoryModification : source.getCosignatoryModifications()) {
			final DbMultisigModification dbModification = this.mapMultisigCosignatoryModification(multisigCosignatoryModification);
			dbModification.setMultisigAggregateModificationTransaction(target);
			multisigModifications.add(dbModification);
		}

		target.setMultisigModifications(multisigModifications);

		final MultisigMinCosignatoriesModification minCosignatoriesModification = source.getMinCosignatoriesModification();
		if (null != minCosignatoriesModification) {
			final DbMultisigMinCosignatoriesModification dbMinCosignatoriesModification = new DbMultisigMinCosignatoriesModification();
			dbMinCosignatoriesModification.setRelativeChange(minCosignatoriesModification.getRelativeChange());
			target.setMultisigMinCosignatoriesModification(dbMinCosignatoriesModification);
		}

		return target;
	}

	private DbMultisigModification mapMultisigCosignatoryModification(final MultisigCosignatoryModification source) {
		final DbAccount cosignatory = this.mapAccount(source.getCosignatory());
		final DbMultisigModification target = new DbMultisigModification();
		target.setCosignatory(cosignatory);
		target.setModificationType(source.getModificationType().value());
		return target;
	}
}
