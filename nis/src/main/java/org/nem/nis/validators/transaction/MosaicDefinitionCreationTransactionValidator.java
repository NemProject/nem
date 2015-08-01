package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates mosaic definition creation transaction.
 * 1. mosaic definition namespace must belong to creator and be active
 * 2. if mosaic is already created (present in mosaic cache), the properties are only allowed to be altered if the creator owns the entire supply.
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
		final MosaicId mosaicId = transaction.getMosaicDefinition().getId();
		final NamespaceId mosaicNamespaceId = mosaicId.getNamespaceId();

		if (!this.namespaceCache.isActive(mosaicNamespaceId, context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		final ReadOnlyNamespaceEntry namespaceEntry = this.namespaceCache.get(mosaicNamespaceId);
		if (!namespaceEntry.getNamespace().getOwner().equals(transaction.getMosaicDefinition().getCreator())) {
			return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
		}

		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
		if (null != mosaicEntry) {
			// creator wants to modify an existing mosaic
			final MosaicProperties currentProperties = mosaicEntry.getMosaicDefinition().getProperties();
			if (!currentProperties.equals(transaction.getMosaicDefinition().getProperties())) {
				final Quantity creatorBalance = mosaicEntry.getBalances().getBalance(transaction.getSigner().getAddress());
				if (!creatorBalance.equals(MosaicUtils.toQuantity(mosaicEntry.getSupply(), currentProperties.getDivisibility()))) {
					return ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED;
				}
			}
		}

		return ValidationResult.SUCCESS;
	}
}
