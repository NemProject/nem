package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a bag of mosaics. For all mosaics in the bag:
 * - [mosaic] underlying mosaic definition must be known
 * - [namespace] underlying namespace must be active at the context height
 * - [mosaic] if mosaic creator is not a participant, then the mosaic definition must have the property 'transferable'
 * - [mosaic] the transaction signer must have enough mosaics
 */
public class MosaicBagValidator implements TSingleTransactionValidator<TransferTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicBagValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		if (!isDivisibilityAllowed(transaction)) {
			return ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED;
		}

		for (final Mosaic mosaic : transaction.getMosaics()) {
			final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaic.getMosaicId());
			if (null == mosaicEntry) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			final MosaicDefinition mosaicDefinition = NamespaceCacheUtils.getMosaicDefinition(this.namespaceCache, mosaic.getMosaicId());
			if (!this.namespaceCache.isActive(mosaicDefinition.getId().getNamespaceId(), context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			final MosaicProperties properties = mosaicDefinition.getProperties();
			if (!isMosaicDefinitionCreatorParticipant(mosaicDefinition, transaction) && !properties.isTransferable()) {
				return ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE;
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean isMosaicDefinitionCreatorParticipant(final MosaicDefinition mosaicDefinition, final TransferTransaction transaction) {
		return mosaicDefinition.getCreator().equals(transaction.getSigner()) || mosaicDefinition.getCreator().equals(transaction.getRecipient());
	}

	private static boolean isDivisibilityAllowed(final TransferTransaction transaction) {
		// allow fractional xem but disallow fractional bags
		final Amount wholeXemAmount = Amount.fromNem(transaction.getAmount().getNumNem());
		return transaction.getMosaics().isEmpty() || wholeXemAmount.equals(transaction.getAmount());
	}
}
