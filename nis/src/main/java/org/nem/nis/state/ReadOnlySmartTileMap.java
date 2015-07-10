package org.nem.nis.state;

import org.nem.core.model.mosaic.*;

/**
 * Interface for readonly smart tile information.
 */
public interface ReadOnlySmartTileMap {

	/**
	 * Gets the size of the smart tile map.
	 *
	 * @return The size.
	 */
	int size();

	/**
	 * Gets a smart tile with given mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 * @return The smart tile.
	 */
	SmartTile get(final MosaicId mosaicId);

	/**
	 * Gets a value indicating whether or not a smart tile with given mosaic id is in the map.
	 *
	 * @param mosaicId The mosaic id.
	 * @return true if a smart tile with the given id exists, false otherwise.
	 */
	boolean contains(final MosaicId mosaicId);
}
