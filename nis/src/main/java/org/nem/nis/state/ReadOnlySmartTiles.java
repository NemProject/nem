package org.nem.nis.state;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Quantity;

/**
 * Read-only smart tile information.
 */
public interface ReadOnlySmartTiles {

	/**
	 * Gets the cache size.
	 *
	 * @return The cache size.
	 */
	int size();

	/**
	 * Get the overall supply of a smart tile for a given mosaic id.
	 * TODO: this can actually be a mosaic name since it is scoped to the namespace part of the mosaic id already
	 *
	 * @param mosaicId The mosaic id.
	 * @return The quantity.
	 */
	Quantity getCurrentSupply(final MosaicId mosaicId);
}
