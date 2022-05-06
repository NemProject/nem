package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

/**
 * Context passed to ImportanceScorer when recalculating importances.
 */
public interface ImportanceScorerContext {

	/**
	 * Gets the importance vector.
	 *
	 * @return The importance vector.
	 */
	ColumnVector getImportanceVector();

	/**
	 * Gets the outlink vector.
	 *
	 * @return The outlink vector.
	 */
	ColumnVector getOutlinkVector();

	/**
	 * Gets the vested balances vector.
	 *
	 * @return The vested balance vector.
	 */
	ColumnVector getVestedBalanceVector();

	/**
	 * Gets the graph weight vector.
	 *
	 * @return The graph weight vector.
	 */
	ColumnVector getGraphWeightVector();
}
