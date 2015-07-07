package org.nem.core.math;

import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.function.DoubleConsumer;

/**
 * Represents a dense matrix.
 */
public final class DenseMatrix extends Matrix {
	private final int numCols;
	private final double[] values;

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

		if (values.length != this.getElementCount()) {
			throw new IllegalArgumentException("incompatible number of values");
		}

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

	// region Matrix abstract functions

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

	class SetWrapper implements DoubleConsumer {
		final int i;
		final int j;

		SetWrapper(final int i, final int j) {
			this.i = i;
			this.j = j;
		}

		@Override
		public void accept(final double value) {
			DenseMatrix.this.setAtUnchecked(this.i, this.j, value);
		}
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		for (int i = 0; i < this.getRowCount(); ++i) {
			for (int j = 0; j < this.getColumnCount(); ++j) {
				final SetWrapper setWrapper = new SetWrapper(i, j);
				func.visit(i, j, this.getAtUnchecked(i, j), setWrapper);
			}
		}
	}

	@Override
	public MatrixNonZeroElementRowIterator getNonZeroElementRowIterator(final int row) {
		return new MatrixNonZeroElementRowIterator() {
			private int index;

			@Override
			public boolean hasNext() {
				for (int i = this.index; i < DenseMatrix.this.getColumnCount(); i++) {
					if (DenseMatrix.this.getAt(row, i) != 0.0) {
						return true;
					}
				}
				return false;
			}

			@Override
			public MatrixElement next() {
				while (this.index < DenseMatrix.this.getColumnCount()) {
					if (DenseMatrix.this.getAt(row, this.index++) != 0.0) {
						return new MatrixElement(row, this.index - 1, DenseMatrix.this.getAt(row, this.index - 1));
					}
				}

				throw new IndexOutOfBoundsException("index out of range");
			}
		};
	}

	// endregion

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		this.forEach((r, c, v) -> {
			if (0 != r && 0 == c) {
				builder.append(System.lineSeparator());
			}

			if (0 != c) {
				builder.append(" ");
			}

			builder.append(format.format(v));
		});

		return builder.toString();
	}
}