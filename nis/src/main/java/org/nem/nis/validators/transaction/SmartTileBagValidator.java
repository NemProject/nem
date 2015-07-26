package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.NamespaceConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a bag of smart tiles. For all smart tiles in the bag:
 * - [mosaic] underlying mosaic must be known
 * - [namespace] underlying namespace must be active at the context height
 * - [mosaic] if mosaic creator is not a participant, then the mosaic must have the property 'transferable'
 * - [mosaic] the transaction signer must have enough smart tiles
 */
public class SmartTileBagValidator implements TSingleTransactionValidator<TransferTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public SmartTileBagValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		for (final MosaicTransferPair pair : transaction.getMosaicTransfers()) {
			final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, pair.getMosaicId());
			if (null == mosaicEntry) {
				return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
			}

			final Mosaic mosaic = NamespaceCacheUtils.getMosaic(this.namespaceCache, pair.getMosaicId());
			if (!this.namespaceCache.isActive(mosaic.getId().getNamespaceId(), context.getBlockHeight())) {
				return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
			}

			final MosaicProperties properties = mosaic.getProperties();
			if (!isMosaicCreatorParticipant(mosaic, transaction) && !properties.isTransferable()) {
				return ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE;
			}

			final Quantity balance = mosaicEntry.getBalances().getBalance(transaction.getSigner().getAddress());
			if (balance.compareTo(pair.getQuantity()) < 0) {
				return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean isMosaicCreatorParticipant(final Mosaic mosaic, final TransferTransaction transaction) {
		return mosaic.getCreator().equals(transaction.getSigner()) || mosaic.getCreator().equals(transaction.getRecipient());
	}
}
