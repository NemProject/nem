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
	 * @param teleportationProbability The teleportation probability.
	 * @param importanceVector The importance (weights).
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public double calculateDangleSum(
			final List<Integer> dangleIndexes,
			final double teleportationProbability,
			final ColumnVector importanceVector) {

		double dangleSum = 0;
		for (final int i : dangleIndexes) {
			dangleSum += importanceVector.getAt(i) * teleportationProbability;
		}

		return dangleSum / importanceVector.size();
	}

	/**
	 * Calculates the final score for all accounts given all POI sub-scores.
	 *
	 * @param importanceVector The importances sub-scores.
	 * @param outlinkVector The out-link sub-scores.
	 * @param vestedBalanceVector The coin-day sub-scores.
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {
		final ColumnVector finalScoreVector = this.calculateScore(importanceVector, outlinkVector, vestedBalanceVector);
		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		// alg is: l1norm(stakes + outlinkWeight*outlinkVector) + importanceWeight * l1norm(PR)
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.1337;

		final ColumnVector weightedOutlinks = outlinkVector.multiply(outlinkWeight).addElementWise(vestedBalanceVector);
		final ColumnVector weightedImportances = importanceVector.multiply(importanceWeight);
		weightedOutlinks.removeNegatives();
		weightedOutlinks.normalize();
		return weightedOutlinks.addElementWise(weightedImportances);
	}
}
