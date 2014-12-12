package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;

/**
 * A repository of all mutable NEM account state.
 */
public interface PoiFacade extends ReadOnlyPoiFacade {

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 */
	public void recalculateImportances(final BlockHeight blockHeight);
}
