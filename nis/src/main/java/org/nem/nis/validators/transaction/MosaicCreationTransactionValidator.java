package org.nem.nis.validators.transaction;

import org.nem.core.model.MosaicCreationTransaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.cache.ReadOnlyMosaicCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates mosaic creation transaction.
 * 1. mosaic namespace must belong to creator
 * 2. mosaic cannot be already created (present in mosaic cache)
 */
public class MosaicCreationTransactionValidator implements TSingleTransactionValidator<MosaicCreationTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;
	private final ReadOnlyMosaicCache mosaicCache;

	public MosaicCreationTransactionValidator(final ReadOnlyNamespaceCache namespaceCache, final ReadOnlyMosaicCache mosaicCache) {
		this.namespaceCache = namespaceCache;
		this.mosaicCache = mosaicCache;
	}

	@Override
	public ValidationResult validate(final MosaicCreationTransaction transaction, final ValidationContext context) {
		final NamespaceId mosaicNamespaceId = transaction.getMosaic().getId().getNamespaceId();

		if (!this.namespaceCache.isActive(mosaicNamespaceId, context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		final Namespace namespace = this.namespaceCache.get(mosaicNamespaceId);
		if (!namespace.getOwner().equals(transaction.getMosaic().getCreator())) {
			return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
		}

		// TODO: mosaic cache test here

		return ValidationResult.SUCCESS;
	}
}
