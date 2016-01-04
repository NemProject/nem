package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;

@SuppressWarnings("unused")
public interface ReadOnlyHistoricalImportances {

	/**
	 * Gets the (historical) importance at the specified block height.
	 *
	 * @param height The block height.
	 * @return The importance.
	 */
	double getHistoricalImportance(BlockHeight height);

	/**
	 * Gets the (historical) page rank at the specified block height.
	 *
	 * @param height The block height.
	 * @return The page rank.
	 */
	double getHistoricalPageRank(final BlockHeight height);

	/**
	 * Gets the size of the historical importances.
	 *
	 * @return The size.
	 */
	int size();
}
