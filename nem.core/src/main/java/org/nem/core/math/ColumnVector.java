package org.nem.core.math;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.function.*;

/**
 * Represents a linear algebra vector.
 */
public class ColumnVector {

	private final int size;
	private final double[] vector;
	private final DenseMatrix matrix;

	/**
	 * Creates a new vector of the specified size.
	 *
	 * @param size The desired size.
	 */
	public ColumnVector(final int size) {
		if (0 == size) {
			throw new IllegalArgumentException("cannot create a vector of zero size");
		}

		this.size = size;
		this.vector = new double[this.size];
		this.matrix = new DenseMatrix(this.size, 1, this.vector);
	}

	/**
	 * Creates a new vector around a raw vector.
	 *
	 * @param vector The vector of data.
	 */
	public ColumnVector(final double... vector) {
		if (null == vector || 0 == vector.length) {
			throw new IllegalArgumentException("vector must not be null and have a non-zero size");
		}

		this.size = vector.length;
		this.vector = vector;
		this.matrix = new DenseMatrix(this.size, 1, this.vector);
	}

	private ColumnVector(final Matrix matrix) {
		// since this is only being called internally, matrix should be a DenseMatrix
		this.matrix = (DenseMatrix)matrix;
		this.vector = this.matrix.getRaw();
		this.size = this.vector.length;
	}

	//region matrix delegation

	//region size / {get|set|increment}At

	/**
	 * Gets the size of the vector.
	 *
	 * @return The size of the vector.
	 */
	public int size() {
		return this.matrix.getRowCount();
	}

	/**
	 * Gets the value at the specified index.
	 *
	 * @param index The index.
	 * @return The value.
	 */
	public double getAt(final int index) {
		return this.matrix.getAt(index, 0);
	}

	/**
	 * Sets a value at the specified index.
	 *
	 * @param index The index.
	 * @param val The value.
	 */
	public void setAt(final int index, final double val) {
		this.matrix.setAt(index, 0, val);
	}

	/**
	 * Increments at the specified index by a value.
	 *
	 * @param index The index.
	 * @param val The value.
	 */
	public void incrementAt(final int index, final double val) {
		this.matrix.incrementAt(index, 0, val);
	}

	//endregion

	//region mutation functions

	/**
	 * Normalizes this vector's elements so that the absolute value of all
	 * elements sums to 1.0.
	 * <br>
	 * This method has the side effect of modifying the implicit context
	 * object, so be careful.
	 */
	public void normalize() {
		this.matrix.normalizeColumns();
	}

	/**
	 * Scales this vector by dividing all of its elements by the specified factor.
	 *
	 * @param scale The scale factor.
	 */
	public void scale(final double scale) {
		this.matrix.scale(scale);
	}

	//endregion

	//region element-wise operations

	/**
	 * Creates a new ColumnVector by multiplying this vector element-wise with
	 * another vector.
	 *
	 * @param vector The vector.
	 * @return The new vector.
	 */
	public ColumnVector multiplyElementWise(final ColumnVector vector) {
		return this.transform(() -> ColumnVector.this.matrix.multiplyElementWise(vector.matrix));
	}

	/**
	 * Creates a new ColumnVector by adding the specified vector to this vector.
	 *
	 * @param vector The specified vector.
	 * @return The new vector.
	 */
	public ColumnVector addElementWise(final ColumnVector vector) {
		return this.transform(() -> ColumnVector.this.matrix.addElementWise(vector.matrix));
	}

	//endregion

	//region aggregation functions

	/**
	 * Gets the sum of the absolute value of all the vector's elements.
	 *
	 * @return The sum of the absolute value of all the vector's elements.
	 */
	public double absSum() {
		return this.matrix.absSum();
	}

	/**
	 * Gets the sum of all the vector's elements.
	 *
	 * @return The sum of all the vectors elements.
	 */
	public double sum() {
		return this.matrix.sum();
	}

	//endregion

	//region transforms

	/**
	 * Creates a new ColumnVector by rounding this vector to the specified number of decimal places.
	 *
	 * @param numPlaces The number of decimal places.
	 * @return The new vector.
	 */
	public ColumnVector roundTo(final int numPlaces) {
		return this.transform(() -> ColumnVector.this.matrix.roundTo(numPlaces));
	}

	/**
	 * Creates a new ColumnVector by adding each element of this vector to a scalar.
	 *
	 * @param scalar The scalar.
	 * @return The new vector.
	 */
	public ColumnVector add(final double scalar) {
		return this.transform(() -> ColumnVector.this.matrix.add(scalar));
	}

	/**
	 * Creates a new ColumnVector by multiplying this vector by a scalar.
	 *
	 * @param scalar The scalar.
	 * @return The new vector.
	 */
	public ColumnVector multiply(final double scalar) {
		return this.transform(() -> ColumnVector.this.matrix.multiply(scalar));
	}

	/**
	 * Creates a new ColumnVector by taking the square root of each element in this vector.
	 *
	 * @return The new vector.
	 */
	public ColumnVector sqrt() {
		return this.transform(ColumnVector.this.matrix::sqrt);
	}

	/**
	 * Creates a new ColumnVector by taking the absolute value of each element in this vector.
	 *
	 * @return The new vector.
	 */
	public ColumnVector abs() {
		return this.transform(ColumnVector.this.matrix::abs);
	}

	private ColumnVector transform(final Supplier<Matrix> supplier) {
		final Matrix matrix = supplier.get();
		return new ColumnVector(matrix);
	}

	//endregion

	//region predicates

	/**
	 * Determines if this vector is a zero vector.
	 *
	 * @return true if this vector is a zero vector.
	 */
	public final boolean isZeroVector() {
		return this.matrix.isZeroMatrix();
	}

	//endregion

	//endregion

	//region getRaw / setAll

	/**
	 * Gets the underlying, raw array.
	 *
	 * @return The underlying, raw array.
	 */
	public double[] getRaw() {
		return this.vector;
	}

	/**
	 * Sets all the vector's elements to the specified value.
	 *
	 * @param val The value.
	 */
	public void setAll(final double val) {
		for (int i = 0; i < this.vector.length; ++i) {
			this.vector[i] = val;
		}
	}

	//endregion

	//region align

	/**
	 * Scales this vector so that v[0] is equal to 1.
	 * This can help PowerIteration converge faster.
	 * Alignment will fail if the vector's first element is 0.
	 *
	 * @return true if the alignment was successful; false otherwise.
	 */
	public boolean align() {
		if (0.0 == this.vector[0]) {
			return false;
		}

		this.scale(this.vector[0]);
		return true;
	}

	//endregion

	//region max / median

	/**
	 * Gets the maximum value for an individual element in this vector.
	 *
	 * @return The maximum value in of this vector.
	 */
	public double max() {
		double maxVal = this.vector[0];
		for (final double val : this.vector) {
			maxVal = Math.max(maxVal, val);
		}

		return maxVal;
	}

	/**
	 * Gets the median value of all elements in this vector.
	 *
	 * @return The median value of all elements in this vector.
	 */
	public double median() {
		final Median median = new Median();
		return median.evaluate(this.vector);
	}

	//endregion

	//region magnitude / distance / correlation

	/**
	 * Gets the magnitude of this vector.
	 *
	 * @return The magnitude of this vector.
	 */
	public double getMagnitude() {
		final ColumnVector nullVector = new ColumnVector(this.size);
		return this.l2Distance(nullVector);
	}

	/**
	 * Calculates the Manhattan distance (L1-norm) between the specified vector and this vector.
	 *
	 * @param vector The specified vector.
	 * @return The Manhattan distance (L1-norm).
	 */
	public double l1Distance(final ColumnVector vector) {
		return this.distance(vector, Math::abs);
	}

	/**
	 * Calculates the Euclidean distance (L2-norm) between the specified vector and this vector.
	 *
	 * @param vector The specified vector.
	 * @return The Euclidean distance.
	 */
	public double l2Distance(final ColumnVector vector) {
		final double distance = this.distance(vector, d -> d * d);
		return Math.sqrt(distance);
	}

	private double distance(final ColumnVector vector, final DoubleFunction<Double> aggregate) {
		if (this.size != vector.size) {
			throw new IllegalArgumentException("cannot determine the distance between vectors with different sizes");
		}

		double distance = 0;
		for (int i = 0; i < this.size; ++i) {
			final double difference = this.vector[i] - vector.vector[i];
			distance += aggregate.apply(difference);
		}

		return distance;
	}

	/**
	 * Calculates the correlation (pearson r) between the specified vector and this vector.
	 *
	 * @param vector The specified vector.
	 * @return The correlation.
	 */
	public double correlation(final ColumnVector vector) {
		if (this.size != vector.size) {
			throw new IllegalArgumentException("cannot determine the correlation between vectors with different sizes");
		}

		final ColumnVector meanAdjustedX = this.meanAdjust();
		final ColumnVector meanAdjustedY = vector.meanAdjust();

		final double squaredDeviationX = meanAdjustedX.multiplyElementWise(meanAdjustedX).sum();
		final double squaredDeviationY = meanAdjustedY.multiplyElementWise(meanAdjustedY).sum();
		final double deviationProduct = meanAdjustedX.multiplyElementWise(meanAdjustedY).sum();
		return deviationProduct / Math.sqrt(squaredDeviationX * squaredDeviationY);
	}

	private ColumnVector meanAdjust() {
		final double mean = this.sum() / this.size;
		return this.add(-mean);
	}

	//endregion

	//region toString

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < this.size; ++i) {
			if (0 != i) {
				builder.append(" ");
			}

			builder.append(format.format(this.vector[i]));
		}

		return builder.toString();
	}

	//endregion

	//region setNegativesToZero

	/**
	 * Sets all negative values to zero.
	 */
	public void removeNegatives() {
		this.matrix.removeNegatives();
	}

	//endregion

	//region hashCode / equals

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.vector);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ColumnVector)) {
			return false;
		}

		final ColumnVector rhs = (ColumnVector)obj;
		return Arrays.equals(this.vector, rhs.vector);
	}

	//endregion
}