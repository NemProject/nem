package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.state.ReadOnlyNamespaceEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates mosaic definition creation transaction.
 * 1. mosaic definition namespace must belong to creator and be active
 * 2. mosaic definition cannot be already created (present in mosaic cache)
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

		if (namespaceEntry.getMosaics().contains(mosaicId)) {
			return ValidationResult.FAILURE_MOSAIC_ALREADY_EXISTS;
		}

		return ValidationResult.SUCCESS;
	}
}
