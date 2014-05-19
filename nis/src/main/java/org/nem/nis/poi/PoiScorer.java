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
	 * @param vestedBalanceVector   The coin-day sub-scores.
	 * @return The weighted teleporation sum of all dangling accounts.
	 */
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector,
			final ScoringAlg scoringAlg) {

		//TODO: For testing use, take out when final scoring alg is decided
//		System.out.println("outLinkVector" + outLinkVector);
//		System.out.println("importanceVector" + importanceVector);
//		System.out.println("vestedBalanceVector" + vestedBalanceVector);

		final ColumnVector finalScoreVector = calculateNonNormalizedScoreVector(
				importanceVector,
				outLinkVector,
				vestedBalanceVector,
				scoringAlg);

		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateNonNormalizedScoreVector(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector,
			final ScoringAlg scoringAlg) {
		switch (scoringAlg) {
			case BLOODYROOKIEOLD:
				return calculateBloodyRookieOldScore(importanceVector, outLinkVector, vestedBalanceVector);

			case UTOPIAN:
				return calculateUtopianFutureScore(importanceVector, outLinkVector, vestedBalanceVector);

			case PYTHON:
				return calculatePythonScore(importanceVector, outLinkVector, vestedBalanceVector);

			case MAKOTO:
				return calculateMakotoScore(importanceVector, outLinkVector, vestedBalanceVector);

			case BLOODYROOKIENEW:
				return calculateBloodyRookieNewScore(importanceVector, outLinkVector, vestedBalanceVector);

			case BLOODYROOKIENEWV2:
			default:
				return calculateBloodyRookieNewV2Score(importanceVector, outLinkVector, vestedBalanceVector);
		}
	}

	private ColumnVector calculateBloodyRookieOldScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {

		final ColumnVector vector = outLinkVector.multiply(2.0).add(vestedBalanceVector);
		vector.normalize();
		return importanceVector
				.multiplyElementWise(vector);
	}

	private ColumnVector calculateBloodyRookieNewScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {

		// final score = l1norm(stakes) + c1 * l1norm(outlinkstrengths) + c2 * l1norm(PR)

		vestedBalanceVector.normalize();
		outLinkVector.normalize();

		double c1 = 0.5;
		double c2 = 0.05;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		return vestedBalanceVector.add(weightedOutlinks).add(weightedImportances);
	}

	private ColumnVector calculateBloodyRookieNewV2Score(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {

		// alg is: l1norm(stakes+ c1*outLinkVector) + c2 * l1norm(PR)

		double c1 = 1.5;
		double c2 = 0.01;

		ColumnVector weightedOutlinks = outLinkVector.multiply(c1).add(vestedBalanceVector);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}

	private ColumnVector calculateUtopianFutureScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {

		// norm(outlink 2 + PR)*stake + sqrt(abs(stake))

		outLinkVector.normalize();
		ColumnVector vector = outLinkVector.multiply(2.0).add(
				importanceVector);
		vector.normalize();

		ColumnVector sqrtcoindays = vestedBalanceVector.sqrt();

		return vector.multiplyElementWise(
				vestedBalanceVector).add(sqrtcoindays);
	}

	private ColumnVector calculatePythonScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {
		// from the original python prototype
		outLinkVector.normalize();
		vestedBalanceVector.normalize();
		return importanceVector.add(outLinkVector)
				.multiplyElementWise(vestedBalanceVector);
	}

	private ColumnVector calculateMakotoScore(
			final ColumnVector importanceVector,
			final ColumnVector outLinkVector,
			final ColumnVector vestedBalanceVector) {
		
		// alg is: l1norm(stakes * c1*outLinkVector) + c2 * l1norm(PR)

		vestedBalanceVector.normalize();
		outLinkVector.normalize();

		System.out.println("normalized vestedBalanceVector: " + vestedBalanceVector);
		System.out.println("normalized outLinkVector: " + outLinkVector);

		double c1 = 2.;
		double c2 = 0.01;

//		ColumnVector weightedOutlinks = outLinkVector.multiply(c1).multiplyElementWise(vestedBalanceVector);
//		ColumnVector weightedImportances = importanceVector.multiply(c2);
		
		ColumnVector weightedOutlinks = outLinkVector.multiply(c1).multiplyElementWise(vestedBalanceVector);
		ColumnVector weightedImportances = importanceVector.multiply(c2);

		weightedOutlinks.normalize();

		return weightedOutlinks.add(weightedImportances);
	}
}
