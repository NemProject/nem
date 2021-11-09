package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

import java.util.List;

/**
 * Static class containing POI utility functions.
 */
public class PoiUtils {

	/**
	 * Calculates the weighted teleporation sum of all dangling accounts.
	 *
	 * @param dangleIndexes The indexes of dangling accounts.
	 * @param teleportationProbability The teleportation probability.
	 * @param importanceVector The importance (weights).
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public static double calculateDangleSum(final List<Integer> dangleIndexes, final double teleportationProbability,
			final ColumnVector importanceVector) {

		double dangleSum = 0;
		for (final int i : dangleIndexes) {
			dangleSum += importanceVector.getAt(i);
		}

		return dangleSum * teleportationProbability / importanceVector.size();
	}
}
