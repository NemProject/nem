package org.nem.nis.validators.transaction;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates provision namespace transactions.
 */
public class ProvisionNamespaceTransactionValidator implements TSingleTransactionValidator<ProvisionNamespaceTransaction> {
	private static final PublicKey LESSOR_PUBLIC_KEY = PublicKey.fromHexString("ab2c76a3725c84f223483361809779961d24bf384157706249eea49a5ff45280");
	public static final Account LESSOR = new Account(Address.fromPublicKey(LESSOR_PUBLIC_KEY));
	public static final Amount ROOT_RENTAL_FEE = Amount.fromNem(25000);
	public static final Amount SUBLEVEL_RENTAL_FEE = Amount.fromNem(1000);

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

			// TODO 20150620 BR -> BR: change this once we know how to handle expiration
			if (!parentNamespace.isActive(context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			if (!parentNamespace.getOwner().equals(transaction.getSigner())) {
				return ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT;
			}

			// TODO 20150620 J-B: isn't this already checked by NamespaceId.isValid?
			// TODO 20150620 BR -> J: no, the NamespaceIdPart does not do this check because a part can be used as root or sublevel part.
			if (NamespaceId.MAX_SUBLEVEL_LENGTH < transaction.getNewPart().toString().length()) {
				return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
			}
		} else {
			if (NamespaceId.MAX_ROOT_LENGTH < transaction.getNewPart().toString().length()) {
				return ValidationResult.FAILURE_NAMESPACE_INVALID_NAME;
			}
		}

		if (!transaction.getLessor().equals(LESSOR)) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_LESSOR;
		}

		final NamespaceId resultingNamespaceId = transaction.getResultingNamespaceId();
		final Namespace namespace = this.namespaceCache.get(resultingNamespaceId);
		if (null != namespace) {
			if (0 != namespace.getId().getLevel()) {
				return ValidationResult.FAILURE_NAMESPACE_ALREADY_EXISTS;
			}

			// TODO 20150620 J-B: why do we want this check?
			// TODO 20150620 BR -> J: I think it was a suggestion on telegram so that people don't waste XEM by double provisioning.
			if (namespace.getExpiryHeight().subtract(context.getBlockHeight()) > 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
				return ValidationResult.FAILURE_NAMESPACE_PROVISION_TOO_EARLY;
			}
		}

		// TODO 20150620 J-B: since these fees are not used by the spam detection, is there any reason for allowing > fees?
		// TODO 20150620 BR -> J: I personally don't mind if the transaction initiator donates to our lessor (from which other things are funded).
		// TODO 20150620 J-B: shouldn't this rental fee validation always be checked?
		// TODO 20150620 BR -> J: you are right, this is a bug, thx.
		final Amount minimalRentalFee = 0 == resultingNamespaceId.getLevel() ? ROOT_RENTAL_FEE : SUBLEVEL_RENTAL_FEE;
		if (minimalRentalFee.compareTo(transaction.getRentalFee()) > 0) {
			return ValidationResult.FAILURE_NAMESPACE_INVALID_RENTAL_FEE;
		}

		return ValidationResult.SUCCESS;
	}
}
