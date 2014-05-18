package org.nem.core.math;

/**
 * Represents a sparse matrix.
 */
public class SparseMatrix extends Matrix {

	private final int numRows;
	private final int numCols;
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
		this.numCols = numCols;
		this.initialCapacityPerRow = initialCapacityPerRow;
		this.values = new double[numRows][];
		this.cols = new int[numRows][];
		this.maxIndices = new int[numRows];
		for (int i=0; i<numRows; i++) {
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
		for (int i=0; i<this.maxIndices[row]; i++) {
			if (this.cols[row][i] == col) {
				return this.values[row][i];
			}
		}

		return 0.0;
	}

	@Override
	protected final void setAtUnchecked(final int row, final int col, final double val) {
		if (val == 0.0) {
			for (int i=0; i<this.maxIndices[row]; i++) {
				if (this.cols[row][i] == col) {
					remove(row, col);
					return;
				}
			}
		} else {
			int size = this.cols[row].length;
			for (int i=0; i<this.maxIndices[row]; i++) {
				if (this.cols[row][i] == col) {
					this.values[row][i] = val;
					return;
				}
			}

			// New column
			if (this.maxIndices[row] == size) {
				reallocate(row);
			}

			this.cols[row][this.maxIndices[row]] = col;
			this.values[row][this.maxIndices[row]] = val;
			this.maxIndices[row] += 1;
		}
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		for (int i = 0; i < this.numRows; ++i) {
			final double[] rowValues = this.values[i];
			final int[] rowCols = this.cols[i];
			final int size = this.maxIndices[i];
			for (int j = 0; j < size; ++j) {
				final int jCopy = j;
				func.visit(i, rowCols[j], rowValues[j], v -> rowValues[jCopy] = v);
			}
		}
	}

	@Override
	protected final void forEach(final ReadOnlyElementVisitorFunction func) {
		for (int i = 0; i < this.numRows; ++i) {
			final double[] rowValues = this.values[i];
			final int[] rowCols = this.cols[i];
			final int size = this.maxIndices[i];
			for (int j = 0; j < size; ++j) {
				func.visit(i, rowCols[j], rowValues[j]);
			}
		}
	}

	//endregion

	/**
	 * Gets the number of non zero columns of a row.
	 *
	 * @return The number of non zero columns.
	 */
	public int getNonZeroColumnCount(int row) {
		return this.maxIndices[row];
	}

	/**
	 * Gets the capacity of a row.
	 *
	 * @return The capacity of the row.
	 */
	public int getRowCapacity(int row) {
		return this.cols[row].length;
	}

	/**
	 * Remove an entries at a specific position
	 * 
	 * @param row The row.
	 * @param col The column.
	 */
	private void remove(int row, int col) {
		// Shrink arrays
		System.arraycopy(this.cols[row], col+1, this.cols[row], col, this.cols[row].length-1-col);
		System.arraycopy(this.values[row], col+1, this.values[row], col, this.values[row].length-1-col);
		this.maxIndices[row] -= 1;
	}
	
	/**
	 * Reallocate the value and column arrays of a row
	 * 
	 * @param row The row.
	 */
	private void reallocate(int row) {
		// Hopefully doesn't happen too often
		int size = this.cols[row].length;
		int[] newCols = new int[size*2];
		double[] newValues = new double[size*2];
		System.arraycopy(this.cols[row], 0, newCols, 0, size);
		System.arraycopy(this.values[row], 0, newValues, 0, size);
		this.cols[row] = newCols;
		this.values[row] = newValues;
	}

	@Override
	public String toString() {
		return String.format("[%d x %d]", this.numRows, this.numCols);
	}
}
