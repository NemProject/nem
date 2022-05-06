package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

/**
 * Helper class that contains functions for calculating POI scores
 */
public class PoiScorer implements ImportanceScorer {

	@Override
	public ColumnVector calculateFinalScore(final ImportanceScorerContext context) {
		final ColumnVector finalScoreVector = this.calculateScore(context);
		finalScoreVector.normalize();
		return finalScoreVector;
	}

	private ColumnVector calculateScore(final ImportanceScorerContext context) {
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.1337;

		// WO: l1norm(max(0, stakes + outlinkWeight*outlinkVector))
		final ColumnVector weightedOutlinks = context.getOutlinkVector().multiply(outlinkWeight)
				.addElementWise(context.getVestedBalanceVector());
		weightedOutlinks.removeNegatives();
		weightedOutlinks.normalize();

		// WI: importanceWeight * PR
		final ColumnVector weightedImportances = context.getImportanceVector().multiply(importanceWeight);

		// (WO + WI) * graphWeightVector
		return weightedOutlinks.addElementWise(weightedImportances).multiplyElementWise(context.getGraphWeightVector());
	}
}
