package org.nem.nis.cache;

import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.Quantity;

/**
 * A smart tile supply cache.
 */
public interface SmartTileSupplyCache extends ReadOnlySmartTileSupplyCache {

	/**
	 * Increases the supply for a given smart tile.
	 *
	 * @param smartTile The smart tile
	 * @return The new supply quantity.
	 */
	Quantity increaseSupply(final SmartTile smartTile);

	/**
	 * Decreases the supply for a given smart tile.
	 *
	 * @param smartTile The smart tile
	 * @return The new supply quantity.
	 */
	Quantity decreaseSupply(final SmartTile smartTile);
}
