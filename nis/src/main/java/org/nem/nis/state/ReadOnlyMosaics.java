package org.nem.nis.state;

import org.nem.core.model.mosaic.*;

/**
 * A read-only mosaics container.
 */
public interface ReadOnlyMosaics {

	/**
	 * Gets the number of mosaics.
	 *
	 * @return The number of mosaics.
	 */
	int size();

	/**
	 * Gets the mosaic object specified by its unique id.
	 *
	 * @param id The mosaic id.
	 * @return The mosaic object.
	 */
	Mosaic get(final MosaicId id);

	/**
	 * Returns a value indicating whether or not the cache contains a mosaic object with the specified unique id.
	 *
	 * @param id The mosaic id.
	 * @return true if a mosaic with the specified unique id exists in the cache, false otherwise.
	 */
	boolean contains(final MosaicId id);
}
