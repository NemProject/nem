package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignerModification;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a db multisig signer modification transfer to a model multisig signer modification transaction.
 */
public class MultisigSignerModificationDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<MultisigSignerModification, MultisigSignerModificationTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignerModificationDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigSignerModificationTransaction mapImpl(final MultisigSignerModification source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);

		final List<MultisigModification> multisigModifications = source.getMultisigModifications().stream()
				.map(this::mapMultisigModification)
				.collect(Collectors.toList());

		return new MultisigSignerModificationTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				multisigModifications);
	}

	private MultisigModification mapMultisigModification(final org.nem.nis.dbmodel.MultisigModification source) {
		final Account cosignatory = this.mapper.map(source.getCosignatory(), Account.class);

		return new MultisigModification(
				MultisigModificationType.fromValueOrDefault(source.getModificationType()),
				cosignatory);
	}
}
