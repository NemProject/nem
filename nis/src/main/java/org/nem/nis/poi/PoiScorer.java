package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.nis.poi.graph.ImportanceScorer;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer implements ImportanceScorer {

	@Override
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector,
			final ColumnVector graphWeightVector) {
		final ColumnVector finalScoreVector = this.calculateScore(importanceVector, outlinkVector, vestedBalanceVector, graphWeightVector);
		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector,
			final ColumnVector graphWeightVector) {
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.1337;

		// WO: l1norm(max(0, stakes + outlinkWeight*outlinkVector))
		final ColumnVector weightedOutlinks = outlinkVector.multiply(outlinkWeight).addElementWise(vestedBalanceVector);
		weightedOutlinks.removeNegatives();
		weightedOutlinks.normalize();

		// WI: importanceWeight * PR
		final ColumnVector weightedImportances = importanceVector.multiply(importanceWeight);

		// (WO + WI) * graphWeightVector
		return weightedOutlinks.addElementWise(weightedImportances).multiplyElementWise(graphWeightVector);
	}
}
