package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A single transaction validator implementation that validates smart tile supply change transactions:
 * - [mosaic] underlying mosaic must be known
 * - [mosaic] transaction signer must be the creator of the mosaic
 * - [mosaic] quantity is either mutable or there was no supply transaction yet
 * - [mosaic] the max quantity is not exceeded
 * - [mosaic] only existing smart tiles owned by the creator can be deleted
 * - [namespace] underlying namespace must be active at the context height
 * -
 */
public class SmartTileSupplyChangeTransactionValidator implements TSingleTransactionValidator<SmartTileSupplyChangeTransaction> {
	private final ReadOnlyNisCache nisCache;

	public SmartTileSupplyChangeTransactionValidator(final ReadOnlyNisCache nisCache) {
		this.nisCache = nisCache;
	}

	@Override
	public ValidationResult validate(final SmartTileSupplyChangeTransaction transaction, final ValidationContext context) {
		final Mosaic mosaic = this.nisCache.getMosaicCache().get(transaction.getMosaicId());
		if (null == mosaic) {
			return ValidationResult.FAILURE_MOSAIC_UNKNOWN;
		}

		if (!mosaic.getCreator().equals(transaction.getSigner())) {
			return ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT;
		}

		final ReadOnlyAccountState state = this.nisCache.getAccountStateCache().findStateByAddress(transaction.getSigner().getAddress());
		final ReadOnlySmartTileMap smartTileMap = state.getSmartTileMap();
		final MosaicProperties properties = mosaic.getProperties();
		final SmartTile existingSmartTile = smartTileMap.get(mosaic.getId());
		if (!properties.isQuantityMutable() && null != existingSmartTile) {
			return ValidationResult.FAILURE_MOSAIC_QUANTITY_IMMUTABLE;
		}

		// TODO 20150710 BR -> all: this is obviously wrong. How do i figure out the overall quantity of all smart tiles with that mosaic id
		// > living in all accounts? We probably need a map where we manage the current quantity of smart tiles for all mosaic ids.
		final Quantity existingQuantity = null == existingSmartTile ? Quantity.ZERO : existingSmartTile.getQuantity();
		if (transaction.getSupplyType().equals(SmartTileSupplyType.CreateSmartTiles) &&
			existingQuantity.add(transaction.getQuantity()).getRaw() > properties.getQuantity()) {
			return ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED;
		}

		if (transaction.getSupplyType().equals(SmartTileSupplyType.DeleteSmartTiles) &&
			existingQuantity.getRaw() < transaction.getQuantity().getRaw()) {
			return ValidationResult.FAILURE_MOSAIC_QUANTITY_NEGATIVE;
		}

		final NamespaceId mosaicNamespaceId = mosaic.getId().getNamespaceId();
		if (!this.nisCache.getNamespaceCache().isActive(mosaicNamespaceId, context.getBlockHeight())) {
			return ValidationResult.FAILURE_NAMESPACE_EXPIRED;
		}

		return ValidationResult.SUCCESS;
	}
}
