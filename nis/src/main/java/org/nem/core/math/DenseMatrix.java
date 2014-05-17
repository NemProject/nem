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

	/**
	 * Creates a new matrix of the specified size and initial values.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 * @param values The initial values.
	 */
	public DenseMatrix(final int rows, final int cols, final double[] values) {
		this(rows, cols);
		this.setAll(values);
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

	/**
	 * Sets all the matrix's elements to the specified values.
	 *
	 * @param values The values.
	 */
	public void setAll(double[] values) {
		final int rows = this.getRowCount();
		final int cols = this.getColumnCount();
		if (values.length != rows * cols)
			throw new IllegalArgumentException("incompatible number of values");

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				this.setAt(i, j, values[i * cols + j]);
			}
		}
	}

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		final int rows = this.getRowCount();
		final int cols = this.getColumnCount();
		for (int i = 0; i < rows; ++i) {
			if (0 != i)
				builder.append(System.lineSeparator());

			for (int j = 0; j < cols; ++j) {
				if (0 != j)
					builder.append(" ");

				builder.append(format.format(this.getAt(i, j)));
			}
		}

		return builder.toString();
	}

	@Override
	public int hashCode() {
		return this.getRowCount() ^ this.getColumnCount();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Matrix))
			return false;

		final Matrix rhs = (Matrix)obj;
		if (!this.isSameSize(rhs))
			return false;

		final int rows = this.getRowCount();
		final int cols = this.getColumnCount();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (this.getAt(i, j) != rhs.getAt(i, j))
					return false;
			}
		}

		return true;
	}
}