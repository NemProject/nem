package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

import java.util.Objects;

/**
 * A single transaction validator implementation that validates mosaic definition creation transaction.
 * 1. mosaic definition namespace must belong to creator and be active
 * 2. if mosaic is already created (present in mosaic cache), the properties are only allowed to be altered if the creator owns the entire supply
 */
public class MosaicDefinitionCreationTransactionValidator implements TSingleTransactionValidator<MosaicDefinitionCreationTransaction> {
	private static final Amount MOSAIC_CREATION_FEE = Amount.fromNem(50000);
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

		if (transaction.getCreationFee().compareTo(MOSAIC_CREATION_FEE) < 0) {
			return ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE;
		}

		if (mosaicDefinition.isMosaicLevyPresent()) {
			final MosaicId feeMosaicId = mosaicDefinition.getMosaicLevy().getMosaicId();
			final ReadOnlyMosaicEntry feeMosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, feeMosaicId);
			if (null == feeMosaicEntry && !mosaicId.equals(feeMosaicId)) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean isModificationAllowed(final ReadOnlyMosaicEntry mosaicEntry, final MosaicDefinition mosaicDefinition) {
		// properties and transfer fee information can only be modified if the mosaic owner owns the entire mosaic supply
		final MosaicDefinition originalDefinition = mosaicEntry.getMosaicDefinition();
		if (!originalDefinition.getProperties().equals(mosaicDefinition.getProperties()) ||
				Objects.equals(originalDefinition.getMosaicLevy(), mosaicDefinition.getMosaicLevy())) {
			return isFullSupplyOwnedByCreator(mosaicEntry);
		}

		// there must be at least one change
		return !mosaicEntry.getMosaicDefinition().getDescriptor().equals(mosaicDefinition.getDescriptor());
	}

	private static boolean isFullSupplyOwnedByCreator(final ReadOnlyMosaicEntry mosaicEntry) {
		final MosaicDefinition mosaicDefinition = mosaicEntry.getMosaicDefinition();
		final Quantity creatorBalance = mosaicEntry.getBalances().getBalance(mosaicDefinition.getCreator().getAddress());
		return creatorBalance.equals(MosaicUtils.toQuantity(mosaicEntry.getSupply(), mosaicDefinition.getProperties().getDivisibility()));
	}
}
