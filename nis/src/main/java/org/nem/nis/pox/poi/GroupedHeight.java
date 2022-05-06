package org.nem.nis.pox.poi;

import org.nem.core.model.primitive.BlockHeight;

/**
 * Static class that exposes a function for calculating grouped height.
 */
public class GroupedHeight {

	/**
	 * Number of blocks that should be treated as a group for POI purposes. In other words, POI importances will only be calculated at
	 * blocks that are a multiple of this grouping number.
	 */
	private static final int POI_GROUPING = 359;

	/**
	 * Calculates the grouped height for the specified height.
	 *
	 * @param height The height.
	 * @return The grouped height.
	 */
	public static BlockHeight fromHeight(final BlockHeight height) {
		final long backInTime = height.getRaw() - 1;
		final long grouped = (backInTime / POI_GROUPING) * POI_GROUPING;
		return 0 == grouped ? BlockHeight.ONE : new BlockHeight(grouped);
	}
}
