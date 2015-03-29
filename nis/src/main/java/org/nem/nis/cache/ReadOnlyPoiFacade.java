package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;

/**
 * A read only facade on top of poi.
 */
public interface ReadOnlyPoiFacade {

	/**
	 * Gets the size of the last poi vector (needed for time synchronization).
	 *
	 * @return The size of the last poi vector.
	 */
	int getLastPoiVectorSize();

	/**
	 * Gets the height at which the last recalculation was (needed for time synchronization).
	 *
	 * @return The the height at which the last recalculation was.
	 */
	BlockHeight getLastPoiRecalculationHeight();
}
