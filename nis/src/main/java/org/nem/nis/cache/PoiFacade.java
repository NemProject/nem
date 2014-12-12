package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.DebitPredicate;

/**
 * A repository of all mutable NEM account state.
 * TODO 20141211 - rename to something else!
 */
public interface PoiFacade extends AccoutStateRepository {

	/**
	 * Gets the size of the last poi vector (needed for time synchronization).
	 *
	 * @return The size of the last poi vector.
	 */
	public int getLastPoiVectorSize();

	/**
	 * Gets the height at which the last recalculation was (needed for time synchronization).
	 *
	 * @return The the height at which the last recalculation was.
	 */
	public BlockHeight getLastPoiRecalculationHeight();

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 */
	public void recalculateImportances(final BlockHeight blockHeight);
}
