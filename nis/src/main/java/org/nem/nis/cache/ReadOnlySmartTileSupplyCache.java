package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Quantity;

/**
 * A readonly smart tile supply cache.
 */
public interface ReadOnlySmartTileSupplyCache {

	/**
	 * Gets the cache size.
	 *
	 * @return The cache size.
	 */
	int size();

	/**
	 * Get the overall supply of a smart tile for a given mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 * @return The quantity.
	 */
	Quantity getCurrentSupply(final MosaicId mosaicId);
}
