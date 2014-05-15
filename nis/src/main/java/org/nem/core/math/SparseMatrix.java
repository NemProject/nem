package org.nem.core.math;


public class SparseMatrix {

	final int numRows;
	final int numCols;

	/**
	 * The rows of the matrix
	 */
	private double[][] values=null;
	private int[][] cols=null;
	private int[] maxIndices=null;

	/**
	 * Creates a new matrix of the specified size which has a given capacity for each row.
	 *
	 * @param numRows The desired number of rows to represent.
	 * @param numCols The desired number of columns to represent.
	 * @param initialCapacity The initial capacity of a row. Choose carefully to avoid reallocation!
	 */
	public SparseMatrix(final int numRows, final int numCols, final int initialCapacityPerRow) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.values = new double[numRows][];
		this.cols = new int[numRows][];
		this.maxIndices = new int[numRows];
		for (int i=0; i<numRows; i++) {
			this.values[i] = new double[initialCapacityPerRow];
			this.cols[i] = new int[initialCapacityPerRow];
			this.maxIndices[i] = 0;
		}
	}
	
	/**
	 * Gets the number of rows.
	 *
	 * @return The number of rows.
	 */
	public int getRowCount() {
		return this.numRows;
	}

	/**
	 * Gets the number of columns.
	 *
	 * @return The number of columns.
	 */
	public int getColumnCount() {
		return this.numCols;
	}

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
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 *
	 * @return The value.
	 */
	public double getAt(final int row, final int col) {
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		for (int i=0; i<this.maxIndices[row]; i++) {
			if (this.cols[row][i] == col) {
				return this.values[row][i];
			}
		}
		return 0.0;
	}

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param value The value.
	 */
	public void setAt(final int row, final int col, final double value) {
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		if (value == 0.0) {
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
					this.values[row][i] = value;
					return;
				}
			}
			
			// New column
			if (this.maxIndices[row] == size) {
				reallocate(row);
			}
			this.cols[row][this.maxIndices[row]] = col;
			this.values[row][this.maxIndices[row]] = value;
			this.maxIndices[row] += 1;			
		}
	}

	/**
	 * Increments a value at the specified row and column by the given value.
	 * 
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final int row, final int col, final double val) {
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		double value = getAt(row, col) + val;
		if (value == 0.0) {
			for (int i=0; i<this.maxIndices[row]; i++) {
				if (this.cols[row][i] == col) {
					remove(row, col);
					return;
				}
			}
		} else {
			setAt(row, col, value);
		}
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeColumns() {
		double[] vector = new double[this.numRows];
		for (int i=0; i<numRows; i++) {
			double[] rowValues = this.values[i];
			int[] rowCols = this.cols[i];
			int size = this.maxIndices[i];
			for (int j=0; j<size; j++) {
				vector[rowCols[j]] += Math.abs(rowValues[j]);
			}
		}
		for (int i=0; i<numRows; i++) {
			double[] rowValues = this.values[i];
			int[] rowCols = this.cols[i];
			int size = this.maxIndices[i];
			for (int j=0; j<size; j++) {
				double norm =  vector[rowCols[j]];
				if (norm > 0) {
					rowValues[j] /= norm;
				}
			}
		}		
	}

	/**
	 * Sum of all the matrix's elements in each row thus forming a vector
	 *
	 * @return The row sum vector.
	 */
	public ColumnVector getRowSumVector() {
		double[] result = new double[this.numRows];
		for (int i=0; i<numRows; i++) {
			double[] rowValues = this.values[i];
			int size = this.maxIndices[i];
			for (int j=0; j<size; j++) {
				result[i] += rowValues[j];
			}
		}
		
		return new ColumnVector(result);
	}

	/**
	 * Multiplies this sparse matrix by a vector.
	 *
	 * @param vector The vector.
	 *
	 * @return The resulting vector.
	 */
	public ColumnVector multiply(final ColumnVector vector) {
		if (this.numCols != vector.size()) {
			throw new IllegalArgumentException("vector size and matrix column count must be equal");
		}
		double[] result = new double[this.numRows];
		double[] rawVector = new double[this.numRows];
		for (int i=0; i<this.numCols; i++) {
			rawVector[i] = vector.getAt(i);
		}
		for (int i=0; i<numRows; i++) {
			double[] rowValues = this.values[i];
			int[] rowCols = this.cols[i];
			int size = this.maxIndices[i];
			double dot=0.0;
			for (int j=0; j<size; j++) {
				dot += rowValues[j] * rawVector[rowCols[j]];
			}
			result[i] = dot;
		}
		
		return new ColumnVector(result);
	}	

	/**
	 * Remove an entries at a specific position
	 * 
	 * @param row
	 * @param col
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
	 * @param row
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
}
