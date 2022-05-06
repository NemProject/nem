package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

/**
 * Interface for the calculating the final importance score.
 */
public interface ImportanceScorer {

	/**
	 * Calculates the final score for all accounts given all POI sub-scores.
	 *
	 * @param context The importance scorer context.
	 * @return The final score vector.
	 */
	ColumnVector calculateFinalScore(final ImportanceScorerContext context);
}
