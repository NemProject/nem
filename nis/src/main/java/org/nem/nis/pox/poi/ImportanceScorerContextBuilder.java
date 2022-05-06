package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

/**
 * A builder for creating an importance scorer context.
 */
public class ImportanceScorerContextBuilder {
	private ColumnVector importanceVector;
	private ColumnVector outlinkVector;
	private ColumnVector vestedBalanceVector;
	private ColumnVector graphWeightVector;

	// region setters

	/**
	 * Sets the importance vector.
	 *
	 * @param importanceVector The importance vector.
	 */
	public void setImportanceVector(final ColumnVector importanceVector) {
		this.importanceVector = importanceVector;
	}

	/**
	 * Sets the outlink vector.
	 *
	 * @param outlinkVector The outlink vector.
	 */
	public void setOutlinkVector(final ColumnVector outlinkVector) {
		this.outlinkVector = outlinkVector;
	}

	/**
	 * Sets the vested balances vector.
	 *
	 * @param vestedBalanceVector The vested balances vector.
	 */
	public void setVestedBalanceVector(final ColumnVector vestedBalanceVector) {
		this.vestedBalanceVector = vestedBalanceVector;
	}

	/**
	 * Sets the graph weight vector.
	 *
	 * @param graphWeightVector The graph weight vector.
	 */
	public void setGraphWeightVector(final ColumnVector graphWeightVector) {
		this.graphWeightVector = graphWeightVector;
	}

	// endregion

	/**
	 * Creates a new importance scorer context.
	 *
	 * @return The importance scorer context
	 */
	public ImportanceScorerContext create() {
		return new ImportanceScorerContext() {

			@Override
			public ColumnVector getImportanceVector() {
				return ImportanceScorerContextBuilder.this.importanceVector;
			}

			@Override
			public ColumnVector getOutlinkVector() {
				return ImportanceScorerContextBuilder.this.outlinkVector;
			}

			@Override
			public ColumnVector getVestedBalanceVector() {
				return ImportanceScorerContextBuilder.this.vestedBalanceVector;
			}

			@Override
			public ColumnVector getGraphWeightVector() {
				return ImportanceScorerContextBuilder.this.graphWeightVector;
			}
		};
	}
}
