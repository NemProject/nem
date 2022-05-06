package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.state.ReadOnlyNamespaceEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates provision namespace transactions:<br>
 * - [non-root] must have a parent in the namespace cache<br>
 * - [non-root] must have an active root<br>
 * - [non-root] must have same owner as parent<br>
 * - [all] must not have a part length exceeding max length<br>
 * - [all] must have default rental fee sink specified<br>
 * - [all] must have a rental fee at least the minimum<br>
 * - [non-root] must not exist<br>
 * - [root] is renewable by owner exclusively expiration +/- one month<br>
 * - [root] is renewable by anyone one month and one day after expiration
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
		final long blocksPerMonth = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerMonth();
		final long blocksPerYear = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerYear();
		if (!isNameValid(transaction)) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
		}

		if (!ReservedNamespaceFilter.isClaimable(transaction.getResultingNamespaceId())) {
			return ValidationResult.FAILURE_NAMESPACE_NOT_CLAIMABLE;
		}

		if (!transaction.getRentalFeeSink().equals(MosaicConstants.NAMESPACE_OWNER_NEM)) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE_SINK;
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

			final BlockHeight expiryHeight = new BlockHeight(namespace.getHeight().getRaw() + blocksPerYear);
			if (expiryHeight.subtract(context.getBlockHeight()) > blocksPerMonth) {
				return ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY;
			}

			// if the transaction signer is not the last owner of the root namespace,
			// block him from leasing the namespace for a month after expiration.
			final Namespace root = this.getNamespace(resultingNamespaceId.getRoot());
			if (!transaction.getSigner().equals(root.getOwner())
					&& expiryHeight.getRaw() + blocksPerMonth > context.getBlockHeight().getRaw()) {
				return ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY;
			}
		}

		final Amount minimalRentalFee = 0 == resultingNamespaceId.getLevel()
				? getRootNamespaceRentalFee(transaction.getVersion(), context.getBlockHeight())
				: getSubNamespaceRentalFee(transaction.getVersion(), context.getBlockHeight());
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

	private static Amount getRootNamespaceRentalFee(final int version, final BlockHeight height) {
		return BlockMarkerConstants.FEE_FORK(version) > height.getRaw()
				? Amount.fromNem(50000)
				: BlockMarkerConstants.SECOND_FEE_FORK(version) > height.getRaw() ? Amount.fromNem(1500) : Amount.fromNem(100);
	}

	private static Amount getSubNamespaceRentalFee(final int version, final BlockHeight height) {
		return BlockMarkerConstants.FEE_FORK(version) > height.getRaw()
				? Amount.fromNem(5000)
				: BlockMarkerConstants.SECOND_FEE_FORK(version) > height.getRaw() ? Amount.fromNem(200) : Amount.fromNem(10);
	}
}
