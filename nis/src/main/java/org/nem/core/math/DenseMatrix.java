package org.nem.core.math;

import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;

/**
 * Represents a linear algebra matrix.
 */
public class DenseMatrix implements Matrix {

	final int rows;
	final int cols;
	final ColumnVector[] columns;

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	public DenseMatrix(final int rows, final int cols) {
		this.rows = rows;
		this.cols = cols;
		this.columns = new ColumnVector[this.cols];
		for (int i = 0; i < this.cols; ++i)
			this.columns[i] = new ColumnVector(this.rows);
	}

	@Override
	public int getRowCount() {
		return this.rows;
	}

	@Override
	public int getColumnCount() {
		return this.cols;
	}

	@Override
	public double getAt(final int row, final int col) {
		return this.columns[col].getAt(row);
	}

	@Override
	public void setAt(final int row, final int col, final double val) {
		this.columns[col].setAt(row, val);
	}

	@Override
	public void incrementAt(final int row, final int col, final double val) {
		this.columns[col].setAt(row, this.columns[col].getAt(row) + val);
	}

	/**
	 * Transposes this matrix.
	 *
	 * @return A transposed matrix.
	 */
	public Matrix transpose() {
		final DenseMatrix transposedMatrix = new DenseMatrix(this.cols, this.rows);
		for (int i = 0; i < this.rows; ++i) {
			for (int j = 0; j < this.cols; ++j) {
				transposedMatrix.columns[i].setAt(j, this.columns[j].getAt(i));
			}
		}

		return transposedMatrix;
	}

	@Override
	public void normalizeColumns() {
		for (int i = 0; i < this.cols; ++i)
			this.columns[i].normalize();
	}

	/**
	 * Gets the sum of the absolute value of all the matrix's elements.
	 *
	 * @return The sum of the absolute value of all the matrix's elements.
	 */
	public double absSum() {
		double sum = 0.0;
		for (int i = 0; i < this.cols; ++i)
			sum += this.columns[i].absSum();

		return sum;
	}

	/**
	 * Gets the sum of all the matrix's elements.
	 *
	 * @return The sum of all the matrix's elements.
	 */
	public double sum() {
		double sum = 0.0;
		for (int i = 0; i < this.cols; ++i)
			sum += this.columns[i].sum();

		return sum;
	}

	/**
	 * Gets the sum of all the matrix's elements in a row.
	 *
	 * @return The sum of all the matrix's elements of a row.
	 */
	public double rowSum(final int row) {
		double sum = 0.0;
		for (int i = 0; i < this.cols; ++i)
			sum += this.columns[i].getAt(row);

		return sum;
	}

	/**
	 * Gets the sum of all the matrix's elements in a column.
	 *
	 * @return The sum of all the matrix's elements of a column.
	 */
	public double columnSum(final int column) {
		return this.columns[column].sum();
	}

	@Override
	public Matrix multiplyElementWise(final Matrix matrix) {
		if (!this.isSameSize(matrix))
			throw new IllegalArgumentException("matrix sizes must be equal");

		final DenseMatrix result = new DenseMatrix(this.rows, this.cols);
		for (int i = 0; i < this.cols; ++i) {
			for (int j = 0; j < this.rows; ++j)
				result.columns[i].setAt(j, result.columns[i].getAt(j) * matrix.getAt(i, j));
		}

		return result;
	}

	/**
	 * Determines if two this matrix and another matrix have the same dimensions.
	 *
	 * @param matrix The other matrix.
	 *
	 * @return true this matrix and the other matrix have the same dimensions.
	 */
	public boolean isSameSize(final Matrix matrix) {
		return this.rows == matrix.getRowCount() && this.cols == matrix.getColumnCount();
	}

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < this.rows; ++i) {
			if (0 != i)
				builder.append(System.lineSeparator());

			for (int j = 0; j < this.cols; ++j) {
				if (0 != j)
					builder.append(" ");

				builder.append(format.format(this.getAt(i, j)));
			}
		}

		return builder.toString();
	}
}