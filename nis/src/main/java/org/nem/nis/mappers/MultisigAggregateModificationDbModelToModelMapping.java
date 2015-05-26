package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a db multisig signer modification transfer to a model multisig signer modification transaction.
 */
public class MultisigAggregateModificationDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbMultisigAggregateModificationTransaction, MultisigAggregateModificationTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigAggregateModificationDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigAggregateModificationTransaction mapImpl(final DbMultisigAggregateModificationTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);

		final List<MultisigCosignatoryModification> multisigCosignatoryModifications = source.getMultisigModifications().stream()
				.map(this::mapMultisigModification)
				.collect(Collectors.toList());

		return new MultisigAggregateModificationTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				multisigCosignatoryModifications);
	}

	private MultisigCosignatoryModification mapMultisigModification(final DbMultisigModification source) {
		final Account cosignatory = this.mapper.map(source.getCosignatory(), Account.class);

		return new MultisigCosignatoryModification(
				MultisigModificationType.fromValueOrDefault(source.getModificationType()),
				cosignatory);
	}
}
