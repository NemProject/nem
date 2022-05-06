package org.nem.nis.state;

import org.nem.core.model.mosaic.MosaicId;

import java.util.Collection;

/**
 * A read-only mosaics container.
 */
@SuppressWarnings("unused")
public interface ReadOnlyMosaics {

	/**
	 * Gets the number of mosaics.
	 *
	 * @return The number of mosaics.
	 */
	int size();

	/**
	 * Gets the total number of mosaics (including versions).
	 *
	 * @return The size.
	 */
	int deepSize();

	/**
	 * Gets the mosaic entry specified by its unique id.
	 *
	 * @param id The mosaic id.
	 * @return The mosaic entry.
	 */
	ReadOnlyMosaicEntry get(final MosaicId id);

	/**
	 * Gets the collection of mosaicIds.
	 *
	 * @return The collection of mosaicIds.
	 */
	Collection<MosaicId> getMosaicIds();

	/**
	 * Returns a value indicating whether or not the cache contains a mosaic object with the specified unique id.
	 *
	 * @param id The mosaic id.
	 * @return true if a mosaic with the specified unique id exists in the cache, false otherwise.
	 */
	boolean contains(final MosaicId id);
}
