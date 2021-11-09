package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.validators.BlockValidator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A block validator that validates:<br>
 * - A mosaic definition creation transaction is not contained in a block with other transactions that affect the same mosaic<br>
 * - A mosaic definition creation transaction is not contained in a block where the referenced namespace is provisioned
 */
public class BlockMosaicDefinitionCreationValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final List<MosaicId> createdMosaicIds = BlockExtensions.streamDefault(block)
				.filter(t -> TransactionTypes.MOSAIC_DEFINITION_CREATION == t.getType())
				.map(t -> ((MosaicDefinitionCreationTransaction) t).getMosaicDefinition().getId()).collect(Collectors.toList());

		final boolean isConflictingTransferPresent = BlockExtensions.streamDefault(block).flatMap(t -> getMosaicIds(t).stream())
				.filter(id -> null != id && createdMosaicIds.contains(id)).findAny().isPresent();

		final List<NamespaceId> referencedNamespaces = createdMosaicIds.stream().map(MosaicId::getNamespaceId).collect(Collectors.toList());

		final boolean isConflictingNamespaceProvisioningPresent = BlockExtensions.streamDefault(block)
				.filter(t -> TransactionTypes.PROVISION_NAMESPACE == t.getType())
				.filter(t -> referencedNamespaces.contains(((ProvisionNamespaceTransaction) t).getResultingNamespaceId())).findAny()
				.isPresent();

		return (isConflictingTransferPresent || isConflictingNamespaceProvisioningPresent)
				? ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION
				: ValidationResult.SUCCESS;
	}

	private static Collection<MosaicId> getMosaicIds(final Transaction transaction) {
		switch (transaction.getType()) {
			case TransactionTypes.MOSAIC_SUPPLY_CHANGE:
				return getMosaicIds((MosaicSupplyChangeTransaction) (transaction));

			case TransactionTypes.TRANSFER:
				return getMosaicIds((TransferTransaction) transaction);
		}

		return Collections.emptyList();
	}

	private static Collection<MosaicId> getMosaicIds(final MosaicSupplyChangeTransaction transaction) {
		return Collections.singletonList(transaction.getMosaicId());
	}

	private static Collection<MosaicId> getMosaicIds(final TransferTransaction transaction) {
		return transaction.getMosaics().stream().map(Mosaic::getMosaicId).collect(Collectors.toList());
	}
}
