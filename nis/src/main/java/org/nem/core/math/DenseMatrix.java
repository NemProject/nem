package org.nem.core.math;

import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;

/**
 * Represents a dense matrix.
 */
public final class DenseMatrix extends Matrix {

	final int numCols;
	final double[] values;

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	public DenseMatrix(final int rows, final int cols) {
		super(rows, cols);
		this.numCols = cols;
		this.values = new double[this.getElementCount()];
	}

	/**
	 * Creates a new matrix of the specified size and values.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 * @param values The specified values.
	 */
	public DenseMatrix(final int rows, final int cols, final double[] values) {
		super(rows, cols);

		if (values.length != this.getElementCount())
			throw new IllegalArgumentException("incompatible number of values");

		this.numCols = cols;
		this.values = values;
	}

	/**
	 * Gets the underlying, raw array.
	 *
	 * @return The underlying, raw array.
	 */
	public double[] getRaw() {
		return this.values;
	}

	//region Matrix abstract functions

	@Override
	protected final Matrix create(final int numRows, final int numCols) {
		return new DenseMatrix(numRows, numCols);
	}

	@Override
	protected final double getAtUnchecked(final int row, final int col) {
		return this.values[row * this.numCols + col];
	}

	@Override
	protected final void setAtUnchecked(final int row, final int col, final double val) {
		this.values[row * this.numCols + col] = val;
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		for (int i = 0; i < this.getRowCount(); ++i) {
			for (int j = 0; j < this.getColumnCount(); ++j) {
				final int iCopy = i;
				final int jCopy = j;
				func.visit(i, j, this.getAtUnchecked(i, j), v -> this.setAtUnchecked(iCopy, jCopy, v));
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