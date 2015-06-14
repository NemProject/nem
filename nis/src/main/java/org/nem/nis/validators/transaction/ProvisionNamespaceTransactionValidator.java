package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates provision namespace transactions.
 */
public class ProvisionNamespaceTransactionValidator implements TSingleTransactionValidator<ProvisionNamespaceTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public ProvisionNamespaceTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final ProvisionNamespaceTransaction transaction, final ValidationContext context) {
		final NamespaceId parent = transaction.getParent();
		if (null != parent) {
			final Namespace parentNamespace = this.namespaceCache.get(parent);
			if (null == parentNamespace) {
				return ValidationResult.FAILURE_NAMESPACE_UNKNOWN;
			}

			// TODO 20150614 BR -> all: here is a fundamental problem. The unconfirmed transactions cache does not know
			// > about the current block height und thus cannot verify if the namespace has expired. An attacker can exploit
			// > this by filling up the cache with transactions that can never be put into a block.
			if (!parentNamespace.isActive(context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			if (!parentNamespace.getOwner().equals(transaction.getSigner().getAddress())) {
				return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
			}

			if (NamespaceId.MAX_SUBLEVEL_LENGTH < transaction.getNewPart().toString().length()) {
				return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
			}
		} else {
			if (NamespaceId.MAX_ROOT_LENGTH < transaction.getNewPart().toString().length()) {
				return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
			}
		}

		return this.namespaceCache.contains(transaction.getResultingNamespaceId())
				? ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS
				: ValidationResult.SUCCESS;
	}
}
