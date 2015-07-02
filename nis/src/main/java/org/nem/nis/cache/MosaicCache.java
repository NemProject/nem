package org.nem.nis.cache;

import org.nem.core.model.mosaic.Mosaic;

/**
 * A mosaic cache.
 */
public interface MosaicCache extends ReadOnlyMosaicCache {

	/**
	 * Adds a mosaic object to the cache.
	 *
	 * @param mosaic The mosaic.
	 */
	void add(final Mosaic mosaic);

	/**
	 * Removes a mosaic object from the cache.
	 *
	 * @param mosaic The mosaic.
	 */
	void remove(final Mosaic mosaic);
}
