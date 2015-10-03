package org.nem.core.math;

import java.util.*;
import java.util.function.*;

/**
 * Abstract matrix class.
 * <br>
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
	public final double getAt(final int row, final int col) {
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
	public final void setAt(final int row, final int col, final double val) {
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
	public final void incrementAt(final int row, final int col, final double val) {
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
	 *
	 * @return The indexes of zero columns.
	 */
	public Collection<Integer> normalizeColumns() {
		final double[] columnSums = this.getColumnSums(Math::abs);
		final List<Integer> zeroColumns = new ArrayList<>();
		for (int i = 0; i < this.numCols; i++) {
			if (0 == columnSums[i]) {
				zeroColumns.add(i);
			}
		}

		this.forEach((row, col, value, setter) -> {
			final double sum = columnSums[col];
			if (0 == sum) {
				return;
			}

			setter.accept(value / sum);
		});

		return zeroColumns;
	}

	/**
	 * Sets all negative values to zero.
	 */
	public void removeNegatives() {
		this.removeLessThan(0);
	}

	/**
	 * Sets all values less than the specified value to zero.
	 *
	 * @param minValue The minimum value that should not be set to zero.
	 */
	public void removeLessThan(final double minValue) {
		this.forEach((row, col, value, setter) -> {
			if (value < minValue) {
				setter.accept(0.0);
			}
		});
	}

	/**
	 * Scales this matrix by dividing all of its elements by the specified factor.
	 *
	 * @param scale The scale factor.
	 */
	public final void scale(final double scale) {
		this.forEach((row, col, value, setter) -> setter.accept(value / scale));
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

	private Matrix join(final Matrix matrix, final boolean isTwoWay, final DoubleBinaryOperator op) {
		if (!this.isSameSize(matrix)) {
			throw new IllegalArgumentException("matrix sizes must be equal");
		}

		final Matrix result = this.create(this.getRowCount(), this.getColumnCount());
		this.forEach((r, c, v) -> result.setAtUnchecked(r, c, op.applyAsDouble(v, matrix.getAtUnchecked(r, c))));

		if (isTwoWay) {
			matrix.forEach((r, c, v) -> result.setAtUnchecked(r, c, op.applyAsDouble(v, this.getAtUnchecked(r, c))));
		}

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
		final double[] sum = new double[] { 0.0 };
		this.forEach((r, c, v) -> sum[0] += op.applyAsDouble(v));
		return sum[0];
	}

	//endregion

	//region vector operations

	/**
	 * Multiplies this matrix by a vector.
	 *
	 * @param vector The vector.
	 * @return The resulting vector.
	 */
	public ColumnVector multiply(final ColumnVector vector) {
		if (this.numCols != vector.size()) {
			throw new IllegalArgumentException("vector size and matrix column count must be equal");
		}

		final double[] rawResult = new double[this.numRows];
		final double[] rawVector = vector.getRaw();

		this.forEach((r, c, v) -> rawResult[r] += v * rawVector[c]);
		return new ColumnVector(rawResult);
	}

	//endregion

	//region transforms

	/**
	 * Transposes this matrix.
	 *
	 * @return A transposed matrix.
	 */
	public final Matrix transpose() {
		final Matrix transposedMatrix = this.create(this.getColumnCount(), this.getRowCount());
		this.forEach((r, c, v) -> transposedMatrix.setAtUnchecked(c, r, v));
		return transposedMatrix;
	}

	/**
	 * Creates a new matrix by rounding all elements in this matrix to the specified number of decimal places.
	 *
	 * @param numPlaces The number of decimal places.
	 * @return The new matrix.
	 */
	public Matrix roundTo(final int numPlaces) {
		final double multipler = Math.pow(10, numPlaces);
		return this.transform(v -> Math.round(v * multipler) / multipler);
	}

	/**
	 * Creates a new matrix by multiplying this matrix by a scalar.
	 *
	 * @param scalar The scalar.
	 * @return The new matrix.
	 */
	public Matrix multiply(final double scalar) {
		return this.transform(v -> v * scalar);
	}

	/**
	 * Creates a new matrix by adding each value in this matrix by a scalar.
	 *
	 * @param scalar The scalar.
	 * @return The new matrix.
	 */
	public Matrix add(final double scalar) {
		return this.transform(v -> v + scalar);
	}

	/**
	 * Creates a new matrix by taking the absolute value of this matrix.
	 *
	 * @return The new matrix.
	 */
	public Matrix abs() {
		return this.transform(Math::abs);
	}

	/**
	 * Creates a new matrix by taking the square root of this matrix.
	 *
	 * @return The new matrix.
	 */
	public Matrix sqrt() {
		return this.transform(Math::sqrt);
	}

	private Matrix transform(final DoubleUnaryOperator op) {
		final Matrix matrix = this.create(this.getRowCount(), this.getColumnCount());
		this.forEach((r, c, v) -> matrix.setAtUnchecked(r, c, op.applyAsDouble(v)));
		return matrix;
	}

	//endregion

	//region predicates

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
		if (row < 0 || row >= this.numRows) {
			throw new IndexOutOfBoundsException("Row index out of bounds");
		}

		if (col < 0 || col >= this.numCols) {
			throw new IndexOutOfBoundsException("Column index out of bounds");
		}
	}

	/**
	 * Determines if this matrix is a zero matrix.
	 *
	 * @return true if this matrix is a zero matrix.
	 */
	public final boolean isZeroMatrix() {
		return 0 == this.absSum();
	}

	//endregion

	//region hashCode / equals

	@Override
	public int hashCode() {
		return this.getRowCount() ^ this.getColumnCount();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Matrix)) {
			return false;
		}

		final Matrix rhs = (Matrix)obj;
		if (!this.isSameSize(rhs)) {
			return false;
		}

		final Matrix inequalityMatrix = this.join(rhs, true, (l, r) -> l == r ? 0 : 1);
		return 0 == inequalityMatrix.sum();
	}

	//endregion

	//region readonly-foreach

	/**
	 * Calls the specified function for every non-zero element.
	 *
	 * @param func The function.
	 */
	public void forEach(final ReadOnlyElementVisitorFunction func) {
		this.forEach((row, col, value, setter) -> func.visit(row, col, value));
	}

	/**
	 * Functional interface that visits every non-zero element in this matrix.
	 * Depending on the implementation, zero elements may or may not be visited.
	 */
	@FunctionalInterface
	public interface ReadOnlyElementVisitorFunction {

		/**
		 * Visits the specified element.
		 *
		 * @param row The row.
		 * @param col The column.
		 * @param value The value.
		 */
		void visit(final int row, final int col, @SuppressWarnings("UnusedParameters") final double value);
	}

	//endregion

	//region abstract functions

	/**
	 * Creates a new matrix.
	 *
	 * @param numRows The number of rows.
	 * @param numCols The number of columns.
	 * @return Created matrix.
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
	protected interface ElementVisitorFunction {

		/**
		 * Visits the specified element.
		 *
		 * @param row The row.
		 * @param col The column.
		 * @param value The value.
		 * @param setter a function that can be used to update the value.
		 */
		void visit(final int row, final int col, final double value, final DoubleConsumer setter);
	}

	/**
	 * Gets the non-zero matrix row element iterator for a given row index.
	 *
	 * @param row The row index.
	 * @return The iterator.
	 */
	public abstract MatrixNonZeroElementRowIterator getNonZeroElementRowIterator(final int row);

	//endregion
}
