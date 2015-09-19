package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;

/**
 * A read only facade on top of pox.
 */
public interface ReadOnlyPoxFacade {

	/**
	 * Gets the size of the last pox vector (needed for time synchronization).
	 *
	 * @return The size of the last pox vector.
	 */
	int getLastVectorSize();

	/**
	 * Gets the height at which the last recalculation was (needed for time synchronization).
	 *
	 * @return The the height at which the last recalculation was.
	 */
	BlockHeight getLastRecalculationHeight();
}
