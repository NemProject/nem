package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

import java.util.Objects;

/**
 * A single transaction validator implementation that validates mosaic definition creation transaction.<br>
 * 1. mosaic definition namespace must belong to creator and be active<br>
 * 2. if mosaic is already created (present in mosaic cache), the properties are only allowed to be altered if the creator owns the entire
 * supply
 */
public class MosaicDefinitionCreationTransactionValidator implements TSingleTransactionValidator<MosaicDefinitionCreationTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicDefinitionCreationTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final MosaicDefinitionCreationTransaction transaction, final ValidationContext context) {
		final MosaicDefinition mosaicDefinition = transaction.getMosaicDefinition();
		final MosaicId mosaicId = mosaicDefinition.getId();
		final NamespaceId mosaicNamespaceId = mosaicId.getNamespaceId();

		if (!this.namespaceCache.isActive(mosaicNamespaceId, context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		final ReadOnlyNamespaceEntry namespaceEntry = this.namespaceCache.get(mosaicNamespaceId);
		if (!namespaceEntry.getNamespace().getOwner().equals(mosaicDefinition.getCreator())) {
			return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
		}

		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
		if (null != mosaicEntry && !isModificationAllowed(mosaicEntry, mosaicDefinition)) {
			return ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED;
		}

		if (!transaction.getCreationFeeSink().equals(MosaicConstants.MOSAIC_CREATION_FEE_SINK)) {
			return ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE_SINK;
		}

		final Amount creationFee = getMosaicCreationFee(transaction.getVersion(), context.getBlockHeight());
		if (transaction.getCreationFee().compareTo(creationFee) < 0) {
			return ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE;
		}

		if (mosaicDefinition.isMosaicLevyPresent()) {
			final MosaicId feeMosaicId = mosaicDefinition.getMosaicLevy().getMosaicId();
			// TODO 20151124 J-B: i guess we also need to check in the transfer mosaic validator
			if (!mosaicId.equals(feeMosaicId)) {
				final ReadOnlyMosaicEntry feeMosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, feeMosaicId);
				if (null == feeMosaicEntry) {
					return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
				}

				return checkLevyProperties(feeMosaicEntry.getMosaicDefinition());
			} else {
				return checkLevyProperties(mosaicDefinition);
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static ValidationResult checkLevyProperties(final MosaicDefinition mosaicDefinition) {
		final MosaicProperties properties = mosaicDefinition.getProperties();
		return properties.isTransferable() ? ValidationResult.SUCCESS : ValidationResult.FAILURE_MOSAIC_LEVY_NOT_TRANSFERABLE;
	}

	private static boolean isModificationAllowed(final ReadOnlyMosaicEntry mosaicEntry, final MosaicDefinition mosaicDefinition) {
		// properties and transfer fee information can only be modified if the mosaic owner owns the entire mosaic supply
		final MosaicDefinition originalDefinition = mosaicEntry.getMosaicDefinition();
		final MosaicProperties originalProperties = originalDefinition.getProperties();
		final MosaicProperties newProperties = mosaicDefinition.getProperties();
		if (!originalProperties.equals(newProperties)
				|| !Objects.equals(originalDefinition.getMosaicLevy(), mosaicDefinition.getMosaicLevy())) {
			return arePropertiesChangesValid(originalProperties, newProperties) && isFullSupplyOwnedByCreator(mosaicEntry);
		}

		// there must be at least one change
		return !mosaicEntry.getMosaicDefinition().getDescriptor().equals(mosaicDefinition.getDescriptor());
	}

	private static boolean arePropertiesChangesValid(final MosaicProperties lhsProperties, final MosaicProperties rhsProperties) {
		// don't allow transferability to change
		return lhsProperties.isTransferable() == rhsProperties.isTransferable();
	}

	private static boolean isFullSupplyOwnedByCreator(final ReadOnlyMosaicEntry mosaicEntry) {
		final MosaicDefinition mosaicDefinition = mosaicEntry.getMosaicDefinition();
		final Quantity creatorBalance = mosaicEntry.getBalances().getBalance(mosaicDefinition.getCreator().getAddress());
		return creatorBalance.equals(MosaicUtils.toQuantity(mosaicEntry.getSupply(), mosaicDefinition.getProperties().getDivisibility()));
	}

	private static Amount getMosaicCreationFee(final int version, final BlockHeight height) {
		return BlockMarkerConstants.FEE_FORK(version) > height.getRaw()
				? Amount.fromNem(50000)
				: BlockMarkerConstants.SECOND_FEE_FORK(version) > height.getRaw() ? Amount.fromNem(500) : Amount.fromNem(10);
	}
}
