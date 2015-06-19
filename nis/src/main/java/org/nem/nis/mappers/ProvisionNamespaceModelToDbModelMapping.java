package org.nem.nis.mappers;

import org.nem.core.model.ProvisionNamespaceTransaction;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model provision namespace transaction to a db provision namespace transaction.
 */
public class ProvisionNamespaceModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ProvisionNamespaceModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbProvisionNamespaceTransaction mapImpl(final ProvisionNamespaceTransaction source) {
		// TODO 20150616 BR -> J: this is obviously not correct, but i don't have the block height to calculate the expiry height here.
		// > Any idea how to fix this?
		// TODO 20150619 J-B: remind me why we want to store the expiry height in the database vs the effective height?
		final DbAccount lessor = this.mapAccount(source.getLessor());
		final Namespace namespace = new Namespace(source.getResultingNamespaceId(), source.getSigner(), BlockHeight.MAX);
		final DbNamespace dbNamespace = this.mapper.map(namespace, DbNamespace.class);
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setLessor(lessor);
		dbTransaction.setRentalFee(source.getRentalFee().getNumMicroNem());
		dbTransaction.setNamespace(dbNamespace);
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
