package org.nem.nis.mappers;

import org.nem.core.model.ProvisionNamespaceTransaction;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model provision namespace transaction to a db provision namespace transaction.
 */
public class ProvisionNamespaceModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<ProvisionNamespaceTransaction, DbProvisionNamespaceTransaction> {

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
		// the namespace height will get overridden by the block mapper so it is ok to set it to some default value here
		final Namespace namespace = new Namespace(source.getResultingNamespaceId(), source.getSigner(), BlockHeight.MAX);
		final DbNamespace dbNamespace = this.mapper.map(namespace, DbNamespace.class);
		dbNamespace.setHeight(null);

		final DbAccount dbRentalFeeSink = this.mapAccount(source.getRentalFeeSink());
		final DbProvisionNamespaceTransaction dbTransaction = new DbProvisionNamespaceTransaction();
		dbTransaction.setRentalFeeSink(dbRentalFeeSink);
		dbTransaction.setRentalFee(source.getRentalFee().getNumMicroNem());
		dbTransaction.setNamespace(dbNamespace);
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
