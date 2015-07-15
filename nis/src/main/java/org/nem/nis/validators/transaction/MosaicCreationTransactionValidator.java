package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates mosaic creation transaction.
 * 1. mosaic namespace must belong to creator and be active
 * 2. mosaic cannot be already created (present in mosaic cache)
 */
public class MosaicCreationTransactionValidator implements TSingleTransactionValidator<MosaicCreationTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicCreationTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final MosaicCreationTransaction transaction, final ValidationContext context) {
		final MosaicId mosaicId = transaction.getMosaic().getId();
		final NamespaceId mosaicNamespaceId = mosaicId.getNamespaceId();

		if (!this.namespaceCache.isActive(mosaicNamespaceId, context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		final ReadOnlyNamespaceEntry namespaceEntry = this.namespaceCache.get(mosaicNamespaceId);
		if (!namespaceEntry.getNamespace().getOwner().equals(transaction.getMosaic().getCreator())) {
			return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
		}

		if (namespaceEntry.getMosaics().contains(mosaicId)) {
			return ValidationResult.FAILURE_MOSAIC_ALREADY_EXISTS;
		}

		return ValidationResult.SUCCESS;
	}
}
