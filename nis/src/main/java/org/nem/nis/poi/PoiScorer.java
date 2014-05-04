package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;

import java.util.List;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer {

	/**
	 * Calculates the weighted teleporation sum of all dangling accounts.
	 *
	 * @param dangleIndexes The indexes of dangling accounts.
	 * @param teleportationVector The teleportation vector.
	 * @param importanceVector The importance (weights).
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public double calculateDangleSum(
			final List<Integer> dangleIndexes,
			final ColumnVector teleportationVector,
			final ColumnVector importanceVector) {

		double dangleSum = 0;
		for (final int i : dangleIndexes)
			dangleSum += importanceVector.getAt(i) * teleportationVector.getAt(i);

		return dangleSum / importanceVector.getSize();
	}

	/**
	 * Calculates the final score for all accounts given all POI sub-scores.
	 *
	 * @param importanceVector The importances sub-scores.
	 * @param outLinkVector The out-link sub-scores.
	 * @param coinDaysVector The coin-day sub-scores.
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {
		final double maxImportance = importanceVector.max();
		final double maxOutLink = outLinkVector.max();
		final double maxCoinDays = coinDaysVector.max();
		final double scale = maxImportance * maxOutLink * maxCoinDays;

		final ColumnVector finalScoreVector = importanceVector
				.multiplyElementWise(outLinkVector)
				.multiplyElementWise(coinDaysVector);

		finalScoreVector.scale(scale);
		return finalScoreVector;
	}
}
