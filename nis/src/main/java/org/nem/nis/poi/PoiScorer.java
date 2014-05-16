package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;

import java.util.List;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer {

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
	 * @param dangleIndexes       The indexes of dangling accounts.
	 * @param teleportationVector The teleportation vector.
	 * @param importanceVector    The importance (weights).
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
	 * @param outLinkVector    The out-link sub-scores.
	 * @param coinDaysVector   The coin-day sub-scores.
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector,
			final ScoringAlg scoringAlg) {

		//TODO: For testing use, take out when final scoring alg is decided
		System.out.println("outLinkVector" + outLinkVector);
		System.out.println("importanceVector" + importanceVector);
		System.out.println("coinDaysVector" + coinDaysVector);

		final ColumnVector finalScoreVector = calculateNonNormalizedScoreVector(
				importanceVector,
				outLinkVector,
				coinDaysVector,
				scoringAlg);

		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateNonNormalizedScoreVector(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector,
			final ScoringAlg scoringAlg) {
		switch (scoringAlg) {
			case BLOODYROOKIEOLD:
				return calculateBloodyRookieOldScore(importanceVector, outLinkVector, coinDaysVector);

			case UTOPIAN:
				return calculateUtopianFutureScore(importanceVector, outLinkVector, coinDaysVector);

			case PYTHON:
				return calculatePythonScore(importanceVector, outLinkVector, coinDaysVector);

			case MAKOTO:
				return calculateMakotoScore(importanceVector, outLinkVector, coinDaysVector);

			case BLOODYROOKIENEW:
				return calculateBloodyRookieNewScore(importanceVector, outLinkVector, coinDaysVector);

			case BLOODYROOKIENEWV2:
			default:
				return calculateBloodyRookieNewV2Score(importanceVector, outLinkVector, coinDaysVector);
		}
	}

	private ColumnVector calculateBloodyRookieOldScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {

		final ColumnVector vector = outLinkVector.multiply(2.0).add(coinDaysVector);
		vector.normalize();
		return importanceVector
				.multiplyElementWise(vector);
	}

	private ColumnVector calculateBloodyRookieNewScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {

		// final score = l1norm(stakes) + c1 * l1norm(outlinkstrengths) + c2 * l1norm(PR)

		coinDaysVector.normalize();
		outLinkVector.normalize();

		double c1 = 0.5;
		double c2 = 0.05;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		return coinDaysVector.add(weightedOutlinks).add(weightedImportances);
	}

	private ColumnVector calculateBloodyRookieNewV2Score(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {

		// alg is: l1norm(stakes+ c1*outLinkVector) + c2 * l1norm(PR)

		coinDaysVector.normalize();
		outLinkVector.normalize();
		
		System.out.println("normalized coinDaysVector: " + coinDaysVector);
		System.out.println("normalized outLinkVector: " + outLinkVector);

		double c1 = 2.;
		double c2 = 0.01;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1).add(coinDaysVector);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}

	private ColumnVector calculateUtopianFutureScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {

		// norm(outlink 2 + PR)*stake + sqrt(stake)

		outLinkVector.normalize();
		ColumnVector vector = outLinkVector.multiply(2.0).add(
				importanceVector);
		vector.normalize();

		ColumnVector sqrtcoindays = coinDaysVector.sqrt();

		return vector.multiplyElementWise(
				coinDaysVector).add(sqrtcoindays);
	}

	private ColumnVector calculatePythonScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {
		// from the original python prototype
		outLinkVector.normalize();
		coinDaysVector.normalize();
		return importanceVector.add(outLinkVector)
				.multiplyElementWise(coinDaysVector);
	}

	private ColumnVector calculateMakotoScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {
		
		// alg is: l1norm(stakes * c1*outLinkVector) + c2 * l1norm(PR)

		coinDaysVector.normalize();
		outLinkVector.normalize();

		System.out.println("normalized coinDaysVector: " + coinDaysVector);
		System.out.println("normalized outLinkVector: " + outLinkVector);

		double c1 = 2.;
		double c2 = 0.01;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1).multiplyElementWise(coinDaysVector);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}
}
