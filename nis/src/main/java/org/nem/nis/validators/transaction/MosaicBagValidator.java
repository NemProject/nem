package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a bag of mosaics. For all mosaics in the bag:<br>
 * - [mosaic] underlying mosaic definition must be known<br>
 * - [namespace] underlying namespace must be active at the context height<br>
 * - [mosaic] if mosaic creator is not a participant, then the mosaic definition must have the property 'transferable'
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
		if (transaction.getMosaics().size() > BlockChainConstants.MAX_ALLOWED_MOSAICS_PER_TRANSFER) {
			return ValidationResult.FAILURE_TOO_MANY_MOSAIC_TRANSFERS;
		}

		if (!isDivisibilityAllowed(transaction)) {
			return ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED;
		}

		for (final Mosaic mosaic : transaction.getMosaics()) {
			final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaic.getMosaicId());
			if (null == mosaicEntry) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			final MosaicDefinition mosaicDefinition = mosaicEntry.getMosaicDefinition();
			if (!this.namespaceCache.isActive(mosaicDefinition.getId().getNamespaceId(), context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			final MosaicProperties properties = mosaicDefinition.getProperties();
			if (!isMosaicDefinitionCreatorParticipant(mosaicDefinition, transaction) && !properties.isTransferable()) {
				return ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE;
			}

			final MosaicLevy levy = mosaicDefinition.getMosaicLevy();
			if (null != levy && null == NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, levy.getMosaicId())) {
				return ValidationResult.FAILURE_MOSAIC_LEVY_UNKNOWN;
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean isMosaicDefinitionCreatorParticipant(final MosaicDefinition mosaicDefinition,
			final TransferTransaction transaction) {
		return mosaicDefinition.getCreator().equals(transaction.getSigner())
				|| mosaicDefinition.getCreator().equals(transaction.getRecipient());
	}

	private static boolean isDivisibilityAllowed(final TransferTransaction transaction) {
		// allow fractional xem but disallow fractional bags
		final Amount wholeXemAmount = Amount.fromNem(transaction.getAmount().getNumNem());
		return transaction.getMosaics().isEmpty() || wholeXemAmount.equals(transaction.getAmount());
	}
}
