package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates smart tile supply change transactions:
 * - [mosaic] underlying mosaic must be known
 * - [namespace] underlying namespace must be active at the context height
 * - [mosaic] transaction signer must be the creator of the mosaic
 * - [mosaic] quantity is mutable
 * - [mosaic] the max quantity is not exceeded
 * - [mosaic] only existing smart tiles owned by the creator can be deleted
 */
public class SmartTileSupplyChangeTransactionValidator implements TSingleTransactionValidator<SmartTileSupplyChangeTransaction> {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public SmartTileSupplyChangeTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final SmartTileSupplyChangeTransaction transaction, final ValidationContext context) {
		final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, transaction.getMosaicId());
		if (null == mosaicEntry) {
			return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
		}

		final Mosaic mosaic = mosaicEntry.getMosaic();
		if (!this.namespaceCache.isActive(mosaic.getId().getNamespaceId(), context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		if (!mosaic.getCreator().equals(transaction.getSigner())) {
			return ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT;
		}

		// TODO 2025-07-22 BR -> J: where do you plan the supply change for mosaics with immutable quantity? During mosaic creation?
		final MosaicProperties properties = mosaic.getProperties();
		if (!properties.isQuantityMutable()) {
			return ValidationResult.FAILURE_MOSAIC_QUANTITY_IMMUTABLE;
		}

		return this.validateQuantityChange(mosaicEntry, transaction);
	}

	private ValidationResult validateQuantityChange(final ReadOnlyMosaicEntry mosaicEntry, final SmartTileSupplyChangeTransaction transaction) {
		final int divisibility = mosaicEntry.getMosaic().getProperties().getDivisibility();
		final Quantity existingQuantity = mosaicEntry.getSupply();
		final Quantity changeQuantity = transaction.getQuantity();
		switch (transaction.getSupplyType()) {
			case CreateSmartTiles:
				if (null == MosaicUtils.tryAdd(divisibility, existingQuantity, changeQuantity)) {
					return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
				}
				break;

			case DeleteSmartTiles:
				final Quantity existingBalance = mosaicEntry.getBalances().getBalance(transaction.getSigner().getAddress());
				if (existingBalance.compareTo(changeQuantity) < 0) {
					return ValidationResult.FAILURE_MOSAIC_QUANTITY_NEGATIVE;
				}
				break;
		}

		return ValidationResult.SUCCESS;
	}
}
