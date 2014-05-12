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
				return calculateMakotoFutureScore(importanceVector, outLinkVector, coinDaysVector);

			case BLOODYROOKIENEW:
			default:
				return calculateBloodyRookieNewScore(importanceVector, outLinkVector, coinDaysVector);
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
		// TODO: in the latest Python prototype, I added outLinkVector instead of multiplying,
		// because I was concerned that otherwise people could boost their importance too easily.
		// Weighting by CoinDays should make this safe enough, though, and multiplying will make the
		// importance more fair to people with less NEM.
		// TODO: write unit tests to study the effect of adding outLinkVector vs. multiplying here.
		//.multiplyElementWise(coinDaysVector);

		// BR: Why scale? this should have no influence on foraging, normalizing seems more natural
		//finalScoreVector.scale(scale); // TODO: This won't work if we add outLinkVector, so keep that in mind.
	}

	private ColumnVector calculateBloodyRookieNewScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {

		//		final score = l1norm(stakes) + c1 * l1norm(outlinkstrengths) + c2 * l1norm(PR)

		coinDaysVector.normalize();
		outLinkVector.normalize();

		double c1 = 0.5;
		double c2 = 0.05;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		return coinDaysVector.add(weightedOutlinks).add(weightedImportances);
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

	private ColumnVector calculateMakotoFutureScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector coinDaysVector) {
		//		final score = l1norm(stakes) + c1 * l1norm(outlinkstrengths) + c2 * l1norm(PR)

		coinDaysVector.normalize();
		outLinkVector.normalize();

		double c1 = 0.5;
		double c2 = 0.05;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		return coinDaysVector.add(weightedOutlinks).add(weightedImportances);
	}
}
