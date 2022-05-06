package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbProvisionNamespaceTransaction;

/**
 * A mapping that is able to map a db provision namespace transaction to a model provision namespace transaction.
 */
public class ProvisionNamespaceDbModelToModelMapping
		extends
			AbstractTransferDbModelToModelMapping<DbProvisionNamespaceTransaction, ProvisionNamespaceTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public ProvisionNamespaceDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	protected ProvisionNamespaceTransaction mapImpl(final DbProvisionNamespaceTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account rentalFeeSink = this.mapper.map(source.getRentalFeeSink(), Account.class);
		final Namespace namespace = this.mapper.map(source.getNamespace(), Namespace.class);
		return new ProvisionNamespaceTransaction(new TimeInstant(source.getTimeStamp()), sender, rentalFeeSink,
				Amount.fromMicroNem(source.getRentalFee()), namespace.getId().getLastPart(), namespace.getId().getParent());
	}
}
