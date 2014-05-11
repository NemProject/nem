package org.nem.core.math;


/**
 * Represents a linear algebra matrix which is sparsely populated.
 */
public class SparseMatrix {

	final int numRows;
	final int numCols;

	/**
	 * The rows of the matrix
	 */
	final private SparseRowVector[] rows;

	/**
	 * Creates a new matrix of the specified size which has a given capacity for each row.
	 *
	 * @param numRows The desired number of rows.
	 * @param numCols The desired number of columns.
	 * @param initialCapacityPerRow The initial capacity for a row's hash map.
	 */
	public SparseMatrix(final int numRows, final int numCols, final int initialCapacityPerRow) {
		this.numRows = numRows;
		this.numCols = numCols;
		rows = new SparseRowVector[numRows];
		for (int i=0; i<numRows; i++) {
			rows[i] = new SparseRowVector(numCols, initialCapacityPerRow);
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
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 *
	 * @return The value.
	 */
	public double getAt(final int row, final int col) {
		return this.rows[row].getAt(col);
	}

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	public void setAt(final int row, final int col, final double val) {
		this.rows[row].setAt(col, val);
	}

	/**
	 * Increments a value at the specified row and column by the given val.
	 * 
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final int row, final int col, final double val) {
		this.rows[row].incrementAt(col, val);
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeColumns() {
		for (int i = 0; i < this.numCols; ++i) {
			double sum = 0.0;
			for (int j=0; j<this.numRows; j++) {
				sum += Math.abs(this.rows[j].getAt(i));
			}
			for (int j=0; j<this.numRows; j++) {
				this.rows[j].setAt(i, this.rows[j].getAt(i)/sum);
			}
		}
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeRows() {
		for (int i = 0; i < this.numRows; ++i)
			this.rows[i].normalize();
	}

	/**
	 * Gets the sum of all the matrix's elements in a row.
	 *
	 * @return The sum of all the matrix's elements of a row.
	 */
	public double rowSum(final int row) {
		return this.rows[row].sum();
	}

	/**
	 * Gets the sum of all the matrix's elements in a column.
	 *
	 * @return The sum of all the matrix's elements of a column.
	 */
	public double columnSum(final int column) {
		double sum = 0.0;
		for (int i=0; i<this.numCols; i++) {
			sum += this.rows[i].getAt(column);
		}
		
		return sum;
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
		
		final ColumnVector result = new ColumnVector(this.numRows);
		for (int i = 0; i<this.numRows; i++) {
			result.setAt(i, this.rows[i].multiply(vector));
		}
		return result;
	}
		
}
