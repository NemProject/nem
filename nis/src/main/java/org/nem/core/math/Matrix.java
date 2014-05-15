package org.nem.core.math;

/**
 * Matrix interface.
 */
public interface Matrix {

	/**
	 * Gets the number of rows.
	 *
	 * @return The number of rows.
	 */
	public int getRowCount();

	/**
	 * Gets the number of columns.
	 *
	 * @return The number of columns.
	 */
	public int getColumnCount();

	/**
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 *
	 * @return The value.
	 */
	public double getAt(final int row, final int col);

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	public void setAt(final int row, final int col, final double val);

	/**
	 * Increments a value at the specified row and column by the given val.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final int row, final int col, final double val);

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeColumns();

	/**
	 * Creates a new Matrix by multiplying this matrix element-wise with
	 * another matrix.
	 *
	 * @param matrix The other matrix.
	 *
	 * @return The new matrix.
	 */
	public Matrix multiplyElementWise(final Matrix matrix);

	/**
	 * Gets the sum of the absolute value of all the matrix's elements.
	 *
	 * @return The sum of the absolute value of all the matrix's elements.
	 */
	public double absSum();

	/**
	 * Gets the sum of all the matrix's elements.
	 *
	 * @return The sum of all the matrix's elements.
	 */
	public double sum();
}
