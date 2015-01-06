package org.nem.nis.mappers;

import org.nem.core.model.MultisigAggregateModificationTransaction;
import org.nem.nis.dbmodel.DbMultisigAggregateModificationTransaction;
import org.nem.nis.dbmodel.DbMultisigModification;

import java.util.HashSet;
import java.util.Set;

/**
 * A mapping that is able to map a model multisig signer modification transaction to a db multisig signer modification transfer.
 */
public class MultisigSignerModificationModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MultisigAggregateModificationTransaction, DbMultisigAggregateModificationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignerModificationModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMultisigAggregateModificationTransaction mapImpl(final MultisigAggregateModificationTransaction source) {
		final DbMultisigAggregateModificationTransaction target = new DbMultisigAggregateModificationTransaction();
		target.setReferencedTransaction(0L);

		final Set<DbMultisigModification> multisigModifications = new HashSet<>(source.getModifications().size());
		for (final org.nem.core.model.MultisigModification multisigModification : source.getModifications()) {
			final DbMultisigModification dbModification = this.mapMultisigModification(multisigModification);
			dbModification.setMultisigAggregateModificationTransaction(target);
			multisigModifications.add(dbModification);
		}

		target.setMultisigModifications(multisigModifications);
		return target;
	}

	private DbMultisigModification mapMultisigModification(final org.nem.core.model.MultisigModification source) {
		final org.nem.nis.dbmodel.Account cosignatory = this.mapAccount(source.getCosignatory());
		final DbMultisigModification target = new DbMultisigModification();
		target.setCosignatory(cosignatory);
		target.setModificationType(source.getModificationType().value());
		return target;
	}
}