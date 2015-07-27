package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;
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

		final MosaicDefinition mosaicDefinition = mosaicEntry.getMosaicDefinition();
		if (!this.namespaceCache.isActive(mosaicDefinition.getId().getNamespaceId(), context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		if (!mosaicDefinition.getCreator().equals(transaction.getSigner())) {
			return ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT;
		}

		final MosaicProperties properties = mosaicDefinition.getProperties();
		if (!properties.isSupplyMutable()) {
			return ValidationResult.FAILURE_MOSAIC_QUANTITY_IMMUTABLE;
		}

		return this.validateQuantityChange(mosaicEntry, transaction);
	}

	private ValidationResult validateQuantityChange(final ReadOnlyMosaicEntry mosaicEntry, final SmartTileSupplyChangeTransaction transaction) {
		final int divisibility = mosaicEntry.getMosaicDefinition().getProperties().getDivisibility();
		final Supply existingSupply = mosaicEntry.getSupply();
		final Supply delta = transaction.getDelta();
		switch (transaction.getSupplyType()) {
			case CreateSmartTiles:
				if (null == MosaicUtils.tryAdd(divisibility, existingSupply, delta)) {
					return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
				}
				break;

			case DeleteSmartTiles:
				final Quantity existingBalance = mosaicEntry.getBalances().getBalance(transaction.getSigner().getAddress());
				final Supply existingBalanceAsSupply = MosaicUtils.toSupply(existingBalance, divisibility);
				if (existingBalanceAsSupply.compareTo(delta) < 0) {
					return ValidationResult.FAILURE_MOSAIC_QUANTITY_NEGATIVE;
				}
				break;
		}

		return ValidationResult.SUCCESS;
	}
}
