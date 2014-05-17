package org.nem.core.math;

import java.util.function.*;

/**
 * Abstract matrix class.
 *
 * This class provides default implementations of most matrix functions
 * but they should be optimized in derived classes when performance is important.
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

	//region get{Element|Row|Column}Count / {get|set|increment}At

	/**
	 * Gets the number of elements.
	 *
	 * @return The number of elements.
	 */
	public final int getElementCount() {
		return this.numRows * this.numCols;
	}

	/**
	 * Gets the number of rows.
	 *
	 * @return The number of rows.
	 */
	public final int getRowCount() {
		return this.numRows;
	}

	/**
	 * Gets the number of columns.
	 *
	 * @return The number of columns.
	 */
	public final int getColumnCount() {
		return this.numCols;
	}

	/**
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @return The value.
	 */
	public double getAt(final int row, final int col) {
		this.checkBounds(row, col);
		return this.getAtUnchecked(row, col);
	}

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
	 * Increments a value at the specified row and column by the given val.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final int row, final int col, final double val) {
		final double originalVal = this.getAt(row, col);
		this.setAtUnchecked(row, col, originalVal + val);
	}

	//endregion

	//region get{Row|Column}SumVector

	/**
	 * Gets a vector containing the sums of each matrix row.
	 *
	 * @return A vector containing the sums of each matrix row.
	 */
	public final ColumnVector getRowSumVector() {
		final double[] sums = new double[this.numRows];
		this.forEach((r, c, v) -> sums[r] += v);
		return new ColumnVector(sums);
	}

	/**
	 * Gets a vector containing the sums of each matrix column.
	 *
	 * @return A vector containing the sums of each matrix column.
	 */
	public final ColumnVector getColumnSumVector() {
		return new ColumnVector(this.getColumnSums(v -> v));
	}

	private double[] getColumnSums(final DoubleUnaryOperator op) {
		final double[] sums = new double[this.numCols];
		this.forEach((r, c, v) -> sums[c] += op.applyAsDouble(v));
		return sums;
	}

	//endregion

	//region mutation functions

	/**
	 * Normalizes each column of the matrix.
	 */
	public final void normalizeColumns() {
		final double[] columnSums = this.getColumnSums(Math::abs);
		this.forEach((r, c, v) -> {
			final double sum = columnSums[c];
			this.setAtUnchecked(r, c, v / (0 == sum ? 1 : sum));
		});
	}

	/**
	 * Scales this matrix by dividing all of its elements by the specified factor.
	 *
	 * @param scale The scale factor.
	 */
	public final void scale(final double scale) {
		this.forEach((r, c, v) -> this.setAtUnchecked(r, c, v / scale));
	}

	//endregion

	//region element-wise operations

	/**
	 * Creates a new Matrix by multiplying this matrix element-wise with
	 * another matrix.
	 *
	 * @param matrix The other matrix.
	 * @return The new matrix.
	 */
	public Matrix multiplyElementWise(final Matrix matrix) {
		return this.join(matrix, false, (l, r) -> l * r);
	}

	/**
	 * Creates a new Matrix by adding this matrix element-wise with
	 * another matrix.
	 *
	 * @param matrix The other matrix.
	 * @return The new matrix.
	 */
	public Matrix addElementWise(final Matrix matrix) {
		return this.join(matrix, true, (l, r) -> l + r);
	}

	private Matrix join(final Matrix matrix, boolean isTwoWay, final DoubleBinaryOperator op) {
		if (!this.isSameSize(matrix))
			throw new IllegalArgumentException("matrix sizes must be equal");

		final Matrix result = this.create(this.getRowCount(), this.getColumnCount());
		this.forEach((r, c, v) -> result.setAtUnchecked(r, c, op.applyAsDouble(v, matrix.getAtUnchecked(r, c))));

		if (isTwoWay)
			matrix.forEach((r, c, v) -> result.setAtUnchecked(r, c, op.applyAsDouble(v, this.getAtUnchecked(r, c))));

		return result;
	}

	//endregion

	//region aggregation functions

	/**
	 * Gets the sum of the absolute value of all the matrix's elements.
	 *
	 * @return The sum of the absolute value of all the matrix's elements.
	 */
	public final double absSum() {
		return this.aggregate(Math::abs);
	}

	/**
	 * Gets the sum of all the matrix's elements.
	 *
	 * @return The sum of all the matrix's elements.
	 */
	public final double sum() {
		return this.aggregate(v -> v);
	}

	private double aggregate(final DoubleUnaryOperator op) {
		// use a double[1] instead of a double so that the sum can be updated by the lambda
		double[] sum = new double[] { 0.0 };
		this.forEach((r, c, v) -> sum[0] += op.applyAsDouble(v));
		return sum[0];
	}

	//endregion

	/**
	 * Transposes this matrix.
	 *
	 * @return A transposed matrix.
	 */
	public final Matrix transpose() {
		final Matrix transposedMatrix = this.create(this.getColumnCount(), this.getRowCount());
		forEach((r, c, v) -> transposedMatrix.setAtUnchecked(c, r, v));
		return transposedMatrix;
	}

	/**
	 * Determines if two this matrix and another matrix have the same dimensions.
	 *
	 * @param matrix The other matrix.
	 * @return true this matrix and the other matrix have the same dimensions.
	 */
	public final boolean isSameSize(final Matrix matrix) {
		return this.numRows == matrix.numRows && this.numCols == matrix.numCols;
	}

	private void checkBounds(final int row, final int col) {
		if (row < 0 || row >= this.numRows)
			throw new IndexOutOfBoundsException("Row index out of bounds");

		if (col < 0 || col >= this.numCols)
			throw new IndexOutOfBoundsException("Column index out of bounds");
	}

	//region abstract functions

	/**
	 * Creates a new matrix.
	 *
	 * @param numRows The number of rows.
	 * @param numCols The number of columns.
	 */
	protected abstract Matrix create(final int numRows, final int numCols);

	/**
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @return The value.
	 */
	protected abstract double getAtUnchecked(final int row, final int col);

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	protected abstract void setAtUnchecked(final int row, final int col, final double val);

	/**
	 * Calls the specified function for every non-zero element.
	 *
	 * @param func The function.
	 */
	protected abstract void forEach(final ElementVisitorFunction func);

	/**
	 * Functional interface that visits every non-zero element in this matrix.
	 * Depending on the implementation, zero elements may or may not be visited.
	 */
	@FunctionalInterface
	public interface ElementVisitorFunction {

		/**
		 * Visits the specified element.
		 *
		 * @param row The row.
		 * @param col The column.
		 * @param value The value.
		 */
		public void visit(int row, int col, double value);
	}

	//endregion
}
