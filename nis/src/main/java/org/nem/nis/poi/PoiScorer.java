package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;

import java.util.List;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer {

	// TODO: might make sense to drop the PoiContext dependency
	private final PoiContext context;

	/**
	 * Creates a new scorer.
	 *
	 * @param context The poi context.
	 */
	public PoiScorer(final PoiContext context) {
		this.context = context;
	}

	/**
	 * Calculates the weighted teleporation sum of all dangling accounts.
	 *
	 * @param importanceVector The importance (weights).
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public double calculateDangleSum(final ColumnVector importanceVector) {
		final List<Integer> dangleIndexes = this.context.getDangleIndexes();
		final ColumnVector teleportationVector = this.context.getTeleportationVector();

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
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector) {
		final ColumnVector coinDaysVector = this.context.getCoinDaysVector();

		final double maxImportance = importanceVector.max();
		final double maxOutLink = outLinkVector.max();
		final double maxCoinDays = coinDaysVector.max();
		final double scale = maxImportance * maxOutLink * maxCoinDays;

		// TODO: making all these copies is definitely NOT efficient
		final ColumnVector finalScoreVector = importanceVector
				.multiplyElementWise(outLinkVector)
				.multiplyElementWise(this.context.getCoinDaysVector());

		finalScoreVector.scale(scale);
		return finalScoreVector;
	}
}
