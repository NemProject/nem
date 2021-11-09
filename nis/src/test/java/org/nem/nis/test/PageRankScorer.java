package org.nem.nis.test;

import org.nem.core.math.ColumnVector;
import org.nem.nis.pox.poi.*;

/**
 * Importance scorer implementation that only uses page rank.
 */
public class PageRankScorer implements ImportanceScorer {

	@Override
	public ColumnVector calculateFinalScore(final ImportanceScorerContext context) {
		final ColumnVector importanceVector = context.getImportanceVector();
		importanceVector.normalize();
		return importanceVector;
	}
}
