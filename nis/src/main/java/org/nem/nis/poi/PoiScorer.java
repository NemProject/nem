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
			dangleSum += importanceVector.getAt(i);
		}

		//TODO: Do we need to also include the interlevel-teleportation prob?
		return dangleSum * teleportationProbability / importanceVector.size();
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
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.1337;

		// WO: l1norm(max(0, stakes + outlinkWeight*outlinkVector))
		final ColumnVector weightedOutlinks = outlinkVector.multiply(outlinkWeight).addElementWise(vestedBalanceVector);
		weightedOutlinks.removeNegatives();
		weightedOutlinks.normalize();

		// WI: importanceWeight * PR
		final ColumnVector weightedImportances = importanceVector.multiply(importanceWeight);

		// WO + WI
		return weightedOutlinks.addElementWise(weightedImportances);
	}
}
