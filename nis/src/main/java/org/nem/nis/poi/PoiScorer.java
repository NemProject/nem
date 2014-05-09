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

		return dangleSum / importanceVector.size();
	}

	/**
	 * Calculates the final score for all accounts given all POI sub-scores.
	 *
	 * @param importanceVector The importances sub-scores.
	 * @param outLinkVector The out-link sub-scores.
	 * @param coinDaysVector The coin-day sub-scores.
	 * @return The weighted teleporation sum of all dangling accounts.
	 * @throws CloneNotSupportedException 
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {
//		final double maxImportance = importanceVector.max();
//		final double maxOutLink = outLinkVector.max();
//		final double maxCoinDays = coinDaysVector.max();
//		final double scale = maxImportance * maxOutLink * maxCoinDays;
		//TODO: we might want to normalize outlinkvector and coindaysvector before this
		ColumnVector vector = outLinkVector.multiply(2.0).add(coinDaysVector);
		vector.normalize();
		final ColumnVector finalScoreVector = importanceVector
				.multiplyElementWise(vector); 
				// TODO: in the latest Python prototype, I added outLinkVector instead of multiplying, 
				// because I was concerned that otherwise people could boost their importance too easily.
				// Weighting by CoinDays should make this safe enough, though, and multiplying will make the 
				// importance more fair to people with less NEM.
				// TODO: write unit tests to study the effect of adding outLinkVector vs. multiplying here.
				//.multiplyElementWise(coinDaysVector);

		// BR: Why scale? this should have no influence on foraging, normalizing seems more natural
		//finalScoreVector.scale(scale); // TODO: This won't work if we add outLinkVector, so keep that in mind.
		finalScoreVector.normalize();
		return finalScoreVector;
	}
}
