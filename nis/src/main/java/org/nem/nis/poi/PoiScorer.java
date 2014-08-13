package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;

import java.util.List;
import java.util.logging.Logger;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer {

	private static final Logger LOGGER = Logger.getLogger(PoiAlphaImportanceGeneratorImpl.class.getName());

	public enum ScoringAlg {
		BLOODYROOKIEOLD,
		BLOODYROOKIENEW,
		BLOODYROOKIENEWV2,
		UTOPIAN,
		PYTHON,
		MAKOTO
	}

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
		for (final int i : dangleIndexes) {
			dangleSum += importanceVector.getAt(i) * teleportationVector.getAt(i);
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
			final ColumnVector vestedBalanceVector,
			final ScoringAlg scoringAlg) {

		//TODO: For testing use, take out when final scoring alg is decided
		LOGGER.finer("outlinkVector" + outlinkVector);
		LOGGER.finer("importanceVector" + importanceVector);
		LOGGER.finer("vestedBalanceVector" + vestedBalanceVector);

		final ColumnVector finalScoreVector = this.calculateNonNormalizedScoreVector(
				importanceVector,
				outlinkVector,
				vestedBalanceVector,
				scoringAlg);

		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateNonNormalizedScoreVector(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector,
			final ScoringAlg scoringAlg) {
		switch (scoringAlg) {
			case BLOODYROOKIEOLD:
				return this.calculateBloodyRookieOldScore(importanceVector, outlinkVector, vestedBalanceVector);

			case UTOPIAN:
				return this.calculateUtopianFutureScore(importanceVector, outlinkVector, vestedBalanceVector);

			case PYTHON:
				return this.calculatePythonScore(importanceVector, outlinkVector, vestedBalanceVector);

			case MAKOTO:
				return this.calculateMakotoScore(importanceVector, outlinkVector, vestedBalanceVector);

			case BLOODYROOKIENEW:
				return this.calculateBloodyRookieNewScore(importanceVector, outlinkVector, vestedBalanceVector);

			case BLOODYROOKIENEWV2:
			default:
				return this.calculateBloodyRookieNewV2Score(importanceVector, outlinkVector, vestedBalanceVector);
		}
	}

	private ColumnVector calculateBloodyRookieOldScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		final ColumnVector vector = outlinkVector.multiply(2.0).add(vestedBalanceVector);
		vector.normalize();
		return importanceVector
				.multiplyElementWise(vector);
	}

	private ColumnVector calculateBloodyRookieNewScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		// final score = l1norm(stakes) + c1 * l1norm(outlinkstrengths) + c2 * l1norm(PR)

		vestedBalanceVector.normalize();
		outlinkVector.normalize();

		final double c1 = 0.5;
		final double c2 = 0.05;

		final ColumnVector weightedOutlinks = outlinkVector.multiply(c1);
		final ColumnVector weightedImportances = importanceVector.multiply(c2);

		return vestedBalanceVector.add(weightedOutlinks).add(weightedImportances);
	}

	private ColumnVector calculateBloodyRookieNewV2Score(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		// alg is: l1norm(stakes+ c1*outlinkVector) + c2 * l1norm(PR)

		final double c1 = 1.5;
		final double c2 = 0.01;

		final ColumnVector weightedOutlinks = outlinkVector.multiply(c1).add(vestedBalanceVector);
		final ColumnVector weightedImportances = importanceVector.multiply(c2);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}

	private ColumnVector calculateUtopianFutureScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		// norm(outlink 2 + PR)*stake + sqrt(abs(stake))

		outlinkVector.normalize();
		final ColumnVector vector = outlinkVector.multiply(2.0).add(
				importanceVector);
		vector.normalize();

		final ColumnVector sqrtcoindays = vestedBalanceVector.sqrt();

		return vector.multiplyElementWise(
				vestedBalanceVector).add(sqrtcoindays);
	}

	private ColumnVector calculatePythonScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {
		// from the original python prototype
		outlinkVector.normalize();
		vestedBalanceVector.normalize();
		return importanceVector.add(outlinkVector)
				.multiplyElementWise(vestedBalanceVector);
	}

	private ColumnVector calculateMakotoScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector) {

		// alg is: l1norm(stakes + outlinkWeight*outlinkVector) + importanceWeight * l1norm(PR)
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.05;

		final ColumnVector weightedOutlinks = outlinkVector.multiply(outlinkWeight).add(vestedBalanceVector);
		final ColumnVector weightedImportances = importanceVector.multiply(importanceWeight);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}
}
