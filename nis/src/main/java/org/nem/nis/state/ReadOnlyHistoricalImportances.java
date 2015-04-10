package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;

public interface ReadOnlyHistoricalImportances {

	/**
	 * Gets the (historical) importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The importance.
	 */
	double getHistoricalImportance(BlockHeight blockHeight);
}
