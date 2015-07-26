package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.state.ReadOnlyNamespaceEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates provision namespace transactions:
 * - [non-root] must have a parent in the namespace cache
 * - [non-root] must have an active root
 * - [non-root] must have same owner as parent
 * - [all] must not have a part length exceeding max length
 * - [all] must have default lessor specified
 * - [all] must have a rental fee at least the minimum
 * - [non-root] must not exist
 * - [root] is renewable by owner exclusively expiration +/- one month
 * - [root] is renewable by anyone one month and one day after expiration
 */
public class ProvisionNamespaceTransactionValidator implements TSingleTransactionValidator<ProvisionNamespaceTransaction> {
	private static final long BLOCKS_PER_YEAR = BlockChainConstants.ESTIMATED_BLOCKS_PER_YEAR;
	private static final long BLOCKS_PER_MONTH = BlockChainConstants.ESTIMATED_BLOCKS_PER_MONTH;
	private static final Amount ROOT_RENTAL_FEE = Amount.fromNem(25000);
	private static final Amount SUBLEVEL_RENTAL_FEE = Amount.fromNem(1000);

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
		if (!isNameValid(transaction)) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
		}

		if (!ReservedNamespaceFilter.isClaimable(transaction.getResultingNamespaceId())) {
			return ValidationResult.FAILURE_NAMESPACE_NOT_CLAIMABLE;
		}

		if (!transaction.getLessor().equals(MosaicConstants.NAMESPACE_OWNER_NEM)) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_LESSOR;
		}

		final NamespaceId parent = transaction.getParent();
		if (null != parent) {
			final Namespace parentNamespace = this.getNamespace(parent);
			if (null == parentNamespace) {
				return ValidationResult.FAILURE_NAMESPACE_UNKNOWN;
			}

			if (!this.namespaceCache.isActive(parent.getRoot(), context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			if (!parentNamespace.getOwner().equals(transaction.getSigner())) {
				return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
			}
		}

		final NamespaceId resultingNamespaceId = transaction.getResultingNamespaceId();
		final Namespace namespace = this.getNamespace(resultingNamespaceId);
		if (null != namespace) {
			if (0 != namespace.getId().getLevel()) {
				return ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS;
			}

			final BlockHeight expiryHeight = new BlockHeight(namespace.getHeight().getRaw() + BLOCKS_PER_YEAR);
			if (expiryHeight.subtract(context.getBlockHeight()) > BLOCKS_PER_MONTH) {
				return ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY;
			}

			// if the transaction signer is not the last owner of the root namespace,
			// block him from leasing the namespace for a month after expiration.
			final Namespace root = this.getNamespace(resultingNamespaceId.getRoot());
			if (!transaction.getSigner().equals(root.getOwner()) && expiryHeight.getRaw() + BLOCKS_PER_MONTH > context.getBlockHeight().getRaw()) {
				return ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY;
			}
		}

		final Amount minimalRentalFee = 0 == resultingNamespaceId.getLevel() ? ROOT_RENTAL_FEE : SUBLEVEL_RENTAL_FEE;
		if (minimalRentalFee.compareTo(transaction.getRentalFee()) > 0) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE;
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean isNameValid(final ProvisionNamespaceTransaction transaction) {
		final int maxLength = null != transaction.getParent() ? NamespaceId.MAX_SUBLEVEL_LENGTH : NamespaceId.MAX_ROOT_LENGTH;
		return maxLength >= transaction.getNewPart().toString().length();
	}

	private Namespace getNamespace(final NamespaceId id) {
		final ReadOnlyNamespaceEntry entry = this.namespaceCache.get(id);
		return null == entry ? null : entry.getNamespace();
	}
}
