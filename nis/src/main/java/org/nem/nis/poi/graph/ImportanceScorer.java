package org.nem.nis.poi.graph;

import org.nem.core.math.ColumnVector;

/**
 * Interface for the calculating the final importance score.
 */
public interface ImportanceScorer {

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
			final ColumnVector vestedBalanceVector);
}
