package org.nem.nis.test;

import org.nem.core.math.ColumnVector;
import org.nem.nis.poi.graph.ImportanceScorer;

/**
 * Importance scorer implementation that only uses page rank.
 */
public class PageRankScorer implements ImportanceScorer {

	@Override
	public ColumnVector calculateFinalScore(
			final ColumnVector importanceVector,
			final ColumnVector outlinkVector,
			final ColumnVector vestedBalanceVector,
			final ColumnVector graphWeightVector) {
		importanceVector.normalize();
		return importanceVector;
	}
}