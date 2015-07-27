package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.nis.validators.BlockValidator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A block validator that validates:
 * - A mosaic creation transaction is not contained in block with other transactions that affect the same mosaic
 */
public class BlockMosaicCreationValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final List<MosaicId> createdMosaicIds = BlockExtensions.streamDefault(block)
				.filter(t -> TransactionTypes.MOSAIC_DEFINITION_CREATION == t.getType())
				.map(t -> ((MosaicDefinitionCreationTransaction)t).getMosaicDefinition().getId())
				.collect(Collectors.toList());

		final boolean isConflictingTransferPresent = BlockExtensions.streamDefault(block)
				.flatMap(t -> getMosaicIds(t).stream())
				.filter(id -> null != id && createdMosaicIds.contains(id))
				.findAny()
				.isPresent();

		return isConflictingTransferPresent ? ValidationResult.FAILURE_CONFLICTING_MOSAIC_CREATION : ValidationResult.SUCCESS;
	}

	private static Collection<MosaicId> getMosaicIds(final Transaction transaction) {
		switch (transaction.getType()) {
			case TransactionTypes.SMART_TILE_SUPPLY_CHANGE:
				return getMosaicIds((SmartTileSupplyChangeTransaction)(transaction));

			case TransactionTypes.TRANSFER:
				return getMosaicIds((TransferTransaction)transaction);
		}

		return Collections.emptyList();
	}

	private static Collection<MosaicId> getMosaicIds(final SmartTileSupplyChangeTransaction transaction) {
		return Collections.singletonList(transaction.getMosaicId());
	}

	private static Collection<MosaicId> getMosaicIds(final TransferTransaction transaction) {
		return transaction.getMosaicTransfers().stream()
				.map(MosaicTransferPair::getMosaicId)
				.collect(Collectors.toList());
	}
}
