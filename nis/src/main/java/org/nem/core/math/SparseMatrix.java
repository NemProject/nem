package org.nem.core.math;

import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Represents a sparse matrix.
 */
public class SparseMatrix extends Matrix {

	private static final double REALLOC_MULTIPLIER = 1.6;

	private final int numRows;
	private final int initialCapacityPerRow;

	/**
	 * The rows of the matrix
	 */
	private double[][] values = null;
	private int[][] cols = null;
	private int[] maxIndices = null;

	/**
	 * Creates a new matrix of the specified size which has a given capacity for each row.
	 *
	 * @param numRows The desired number of rows to represent.
	 * @param numCols The desired number of columns to represent.
	 * @param initialCapacityPerRow The initial capacity of a row. Choose carefully to avoid reallocation!
	 */
	public SparseMatrix(final int numRows, final int numCols, final int initialCapacityPerRow) {
		super(numRows, numCols);
		this.numRows = numRows;
		this.initialCapacityPerRow = initialCapacityPerRow;
		this.values = new double[numRows][];
		this.cols = new int[numRows][];
		this.maxIndices = new int[numRows];
		for (int i = 0; i < numRows; ++i) {
			this.values[i] = new double[initialCapacityPerRow];
			this.cols[i] = new int[initialCapacityPerRow];
			this.maxIndices[i] = 0;
		}
	}

	//region Matrix abstract functions

	@Override
	protected final Matrix create(final int numRows, final int numCols) {
		return new SparseMatrix(numRows, numCols, this.initialCapacityPerRow);
	}

	@Override
	protected final double getAtUnchecked(final int row, final int col) {
		final int i = Arrays.binarySearch(this.cols[row], 0, this.maxIndices[row], col);
		return i < 0 ? 0.0 : this.values[row][i];
	}

	@Override
	protected final void setAtUnchecked(final int row, final int col, final double val) {
		// keep the columns sorted in ascending order (needed for SparseBitmap in NodeNeighborhoodMap ctor)

		if (val == 0.0) {
			for (int i = 0; i < this.maxIndices[row]; ++i) {
				if (this.cols[row][i] == col) {
					this.remove(row, i);
					return;
				}
			}
			return;
		}

		if (this.maxIndices[row] == 0) {
			this.cols[row][0] = col;
			this.values[row][0] = val;
			this.maxIndices[row] += 1;
			return;
		}

		int i = 0;
		final int maxIndex = this.maxIndices[row];
		while (i < maxIndex && this.cols[row][i] < col) {
			i++;
		}

		if (i == maxIndex) {
			if (maxIndex == this.cols[row].length) {
				this.reallocate(row);
			}

			this.cols[row][i] = col;
			this.values[row][i] = val;
			this.maxIndices[row] += 1;
			return;
		}

		if (this.cols[row][i] == col) {
			this.values[row][i] = val;
			return;
		}

		this.insertColumn(row, col, val, i);
	}

	private void insertColumn(final int row, final int col, final double val, final int i) {
		if (this.maxIndices[row] == this.cols[row].length) {
			this.reallocate(row);
		}

		System.arraycopy(this.cols[row], i, this.cols[row], i + 1, this.maxIndices[row] - i);
		System.arraycopy(this.values[row], i, this.values[row], i + 1, this.maxIndices[row] - i);
		this.cols[row][i] = col;
		this.values[row][i] = val;
		this.maxIndices[row] += 1;
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		final boolean[] copied = new boolean[1];
		for (int i = 0; i < this.numRows; ++i) {
			final int iCopy = i;
			final double[] rowValues = this.values[i];
			final int[] rowCols = this.cols[i];
			int size = this.maxIndices[i];
			for (int j = 0; j < size; ++j) {
				final int jCopy = j;
				copied[0] = false;
				func.visit(i, rowCols[j], rowValues[j], v -> {
					if (0.0 == v) {
						copied[0] = this.remove(iCopy, jCopy);
					} else {
						rowValues[jCopy] = v;
					}
				});

				if (copied[0]) {
					// Same index again since the array shrank.
					size = this.maxIndices[i];
					--j;
				}
			}
		}
	}

	@Override
	public final void forEach(final ReadOnlyElementVisitorFunction func) {
		for (int i = 0; i < this.numRows; ++i) {
			final double[] rowValues = this.values[i];
			final int[] rowCols = this.cols[i];
			final int size = this.maxIndices[i];
			for (int j = 0; j < size; ++j) {
				func.visit(i, rowCols[j], rowValues[j]);
			}
		}
	}

	@Override
	public MatrixNonZeroElementRowIterator getNonZeroElementRowIterator(final int row) {
		final int[] cols = this.cols[row];
		final double[] values = this.values[row];
		final int maxIndex = this.maxIndices[row];
		return new MatrixNonZeroElementRowIterator() {
			private int index;

			@Override
			public boolean hasNext() {
				return this.index < maxIndex;
			}

			@Override
			public MatrixElement next() {
				if (!this.hasNext()) {
					throw new IndexOutOfBoundsException("index out of range");
				}

				return new MatrixElement(row, cols[this.index], values[this.index++]);
			}
		};
	}

	//endregion

	/**
	 * Gets the number of non zero columns of a row.
	 *
	 * @param row The row.
	 * @return The number of non zero columns.
	 */
	public int getNonZeroColumnCount(final int row) {
		return this.maxIndices[row];
	}

	/**
	 * Gets the capacity of a row.
	 *
	 * @param row The row.
	 * @return The capacity of the row.
	 */
	public int getRowCapacity(final int row) {
		return this.cols[row].length;
	}

	/**
	 * Remove an entries at a specific position
	 *
	 * @param row The row.
	 * @param colIndex The column index.
	 */
	private boolean remove(final int row, final int colIndex) {
		// Shrink arrays
		boolean copied = false;
		final int lastIndex = this.maxIndices[row] - 1;
		if (lastIndex > 0 && lastIndex != colIndex) {
			// We have to copy since the values within one row are ordered.
			System.arraycopy(this.cols[row], colIndex + 1, this.cols[row], colIndex, lastIndex - colIndex);
			System.arraycopy(this.values[row], colIndex + 1, this.values[row], colIndex, lastIndex - colIndex);
			copied = true;
		}

		if (lastIndex >= 0) {
			// Keep the arrays clean, it doesn't cost much time.
			this.cols[row][lastIndex] = 0;
			this.values[row][lastIndex] = 0.0;
		}

		--this.maxIndices[row];
		return copied;
	}

	/**
	 * Reallocate the value and column arrays of a row
	 *
	 * @param row The row.
	 */
	private void reallocate(final int row) {
		// Hopefully doesn't happen too often
		final int size = this.cols[row].length;
		final int newSize = (int)Math.ceil(REALLOC_MULTIPLIER * size);
		final int[] newCols = new int[newSize];
		final double[] newValues = new double[newSize];
		System.arraycopy(this.cols[row], 0, newCols, 0, size);
		System.arraycopy(this.values[row], 0, newValues, 0, size);
		this.cols[row] = newCols;
		this.values[row] = newValues;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("[%d x %d]", this.getRowCount(), this.getColumnCount()));

		this.forEach((r, c, v) -> builder.append(formatEntry(r, c, v)));
		return builder.toString();
	}

	private static String formatEntry(final int row, final int col, final double value) {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		return String.format(
				"%s(%d, %d) -> %s",
				System.lineSeparator(),
				row,
				col,
				format.format(value));
	}

	/**
	 * Returns the number of entries (values that have been set) in this sparse matrix.
	 *
	 * @return number of entries in this sparse matrix.
	 */
	public int getNumEntries() {
		int numEntries = 0;
		for (int i = 0; i < this.getRowCount(); i++) {
			numEntries += this.maxIndices[i];
		}
		return numEntries;
	}
}
