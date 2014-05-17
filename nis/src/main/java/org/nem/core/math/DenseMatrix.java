package org.nem.core.math;

import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;

/**
 * Represents a dense matrix.
 */
public final class DenseMatrix extends Matrix {

	final ColumnVector[] columns;

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	public DenseMatrix(final int rows, final int cols) {
		super(rows, cols);
		this.columns = new ColumnVector[cols];
		for (int i = 0; i < cols; ++i)
			this.columns[i] = new ColumnVector(rows);
	}

	//region Matrix abstract functions

	@Override
	protected final Matrix create(final int numRows, final int numCols) {
		return new DenseMatrix(numRows, numCols);
	}

	@Override
	protected final double getAtUnchecked(final int row, final int col) {
		return this.columns[col].getAt(row);
	}

	@Override
	protected final void setAtUnchecked(final int row, final int col, final double val) {
		this.columns[col].setAt(row, val);
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		for (int i = 0; i < this.getRowCount(); ++i) {
			for (int j = 0; j < this.getColumnCount(); ++j) {
				func.visit(i, j, this.getAtUnchecked(i, j));
			}
		}
	}

	//endregion

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		this.forEach((r, c, v) -> {
			if (0 != r && 0 == c)
				builder.append(System.lineSeparator());

			if (0 != c)
				builder.append(" ");

			builder.append(format.format(v));
		});

		return builder.toString();
	}
}