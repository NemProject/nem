package org.nem.core.math;

/**
 * Abstract matrix class.
 */
public abstract class Matrix {

	private final int numRows;
	private final int numCols;

	/**
	 * Creates a new matrix.
	 *
	 * @param numRows The number of rows.
	 * @param numCols The number of columns.
	 */
	protected Matrix(final int numRows, final int numCols) {
		this.numRows = numRows;
		this.numCols = numCols;
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
		this.checkBounds(row, col);
		return this.getAtUnchecked(row, col);
	}

	/**
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 *
	 * @return The value.
	 */
	public abstract double getAtUnchecked(final int row, final int col);

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	public void setAt(final int row, final int col, final double val) {
		this.checkBounds(row, col);
		this.setAtUnchecked(row, col, val);
	}

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	public abstract void setAtUnchecked(final int row, final int col, final double val);

	/**
	 * Increments a value at the specified row and column by the given val.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final int row, final int col, final double val) {
		final double originalVal = this.getAt(row, col);
		this.setAt(row, col, originalVal + val);
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public abstract void normalizeColumns();

	/**
	 * Creates a new Matrix by multiplying this matrix element-wise with
	 * another matrix.
	 *
	 * @param matrix The other matrix.
	 *
	 * @return The new matrix.
	 */
	public abstract Matrix multiplyElementWise(final Matrix matrix);

	/**
	 * Gets the sum of the absolute value of all the matrix's elements.
	 *
	 * @return The sum of the absolute value of all the matrix's elements.
	 */
	public abstract double absSum();

	/**
	 * Gets the sum of all the matrix's elements.
	 *
	 * @return The sum of all the matrix's elements.
	 */
	public abstract double sum();

	/**
	 * Determines if two this matrix and another matrix have the same dimensions.
	 *
	 * @param matrix The other matrix.
	 *
	 * @return true this matrix and the other matrix have the same dimensions.
	 */
	public boolean isSameSize(final Matrix matrix) {
		return this.numRows == matrix.getRowCount() && this.numCols == matrix.getColumnCount();
	}

	private void checkBounds(final int row, final int col) {
		if (row < 0 || row >= this.numRows)
			throw new IllegalArgumentException("Row index out of bounds");

		if (col < 0 || col >= this.numCols)
			throw new IllegalArgumentException("Column index out of bounds");
	}
}
