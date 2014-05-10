package org.nem.core.math;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.function.DoubleFunction;

/**
 * Represents a linear algebra vector.
 */
public class ColumnVector implements Cloneable {

	private final int size;
	private final double[] vector;

	/**
	 * Creates a new vector of the specified size.
	 *
	 * @param size The desired size.
	 */
	public ColumnVector(final int size) {
		if (0 == size)
			throw new IllegalArgumentException("cannot create a vector of zero size");

		this.size = size;
		this.vector = new double[this.size];
	}

	/**
	 * Creates a new vector around a raw vector.
	 *
	 * @param vector The vector of data.
	 */
	public ColumnVector(final double... vector) {
		if (null == vector || 0 == vector.length)
			throw new IllegalArgumentException("vector must not be null and have a non-zero size");

		this.size = vector.length;
		this.vector = vector;
	}

	/**
	 * Gets the size of the vector.
	 *
	 * @return The size of the vector.
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Gets the value at the specified index.
	 *
	 * @param index The index.
	 *
	 * @return The value.
	 */
	public double getAt(final int index) {
		return this.vector[index];
	}

	/**
	 * Sets a value at the specified index.
	 *
	 * @param index The index.
	 * @param val   The value.
	 */
	public void setAt(final int index, double val) {
		this.vector[index] = val;
	}

	/**
	 * Sets all the vector's elements to the specified value.
	 *
	 * @param val The value.
	 */
	public void setAll(double val) {
		for (int i = 0; i < this.vector.length; ++i)
			this.vector[i] = val;
	}

	/**
	 * Gets the sum of all the vector's elements.
	 *
	 * @return The sum of all the vectors elements.
	 */
	public double sum() {
		double sum = 0.0;
		for (double value : this.vector)
			sum += value;

		return sum;
	}

	/**
	 * Gets the sum of the absolute value of all the vector's elements.
	 *
	 * @return The sum of the absolute value of all the vector's elements.
	 */
	public double absSum() {
		double sum = 0.0;
		for (double value : this.vector)
			sum += Math.abs(value);

		return sum;
	}

	/**
	 * Scales this vector so that v[0] is equal to 1.
	 * This can help PowerIteration converge faster.
	 * Alignment will fail if the vector's first element is 0.
	 *
	 * @return true if the alignment was successful; false otherwise.
	 */
	public boolean align() {
		if (0.0 == this.vector[0])
			return false;

		this.scale(this.vector[0]);
		return true;
	}

	/**
	 * Normalizes this vector's elements so that the absolute value of all
	 * elements sums to 1.0.
	 * 
	 * This method has the side effect of modifying the implicit context 
	 * object, so be careful.
	 */
	public void normalize() {
		double sum = this.absSum();
		if (0.0 == sum)
			return;

		this.scale(sum);
	}

	/**
	 * Scales this vector by dividing all of its elements by the specified factor.
	 *
	 * @param scale The scale factor.
	 */
	public void scale(final double scale) {
		for (int i = 0; i < this.size; ++i)
			this.vector[i] /= scale;
	}

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
	 * Gets the maximum value for an individual element in this vector.
	 *
	 * @return The maximum value in of this vector.
	 */
	public double max() {
		double maxVal = this.vector[0];
		for (double val : this.vector) {
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

	/**
	 * Creates a new ColumnVector by adding the specified vector to this vector.
	 *
	 * @param vector The specified vector.
	 *
	 * @return The new vector.
	 */
	public ColumnVector add(final ColumnVector vector) {
		if (this.size != vector.size)
			throw new IllegalArgumentException("cannot add vectors with different sizes");

		final ColumnVector result = new ColumnVector(this.size);
		for (int i = 0; i < this.size; ++i)
			result.vector[i] = this.vector[i] + vector.vector[i];

		return result;
	}
	
	/**
	 * Creates a new ColumnVector by taking the square root of each element in this vector.
	 * We take the absolute value of each element and then take its square root.
	 *
	 * @return The new vector.
	 */
	public ColumnVector sqrt() {
		final ColumnVector result = new ColumnVector(this.size);
		for (int i = 0; i < this.size; ++i)
			result.vector[i] = Math.sqrt(Math.abs(this.vector[i]));

		return result;
	}

	/**
	 * Creates a new ColumnVector by multiplying this vector element-wise with
	 * another vector.
	 *
	 * @param vector The vector.
	 *
	 * @return The new vector.
	 */
	public ColumnVector multiplyElementWise(final ColumnVector vector) {
		if (this.size != vector.size)
			throw new IllegalArgumentException("vector sizes must be equal");

		final ColumnVector result = new ColumnVector(this.size);
		for (int i = 0; i < this.size; ++i)
			result.vector[i] = this.vector[i] * vector.vector[i];

		return result;
	}

	/**
	 * Creates a new ColumnVector by multiplying this vector by a scalar.
	 *
	 * @param scalar The scalar.
	 *
	 * @return The new vector.
	 */
	public ColumnVector multiply(final double scalar) {
		final ColumnVector result = new ColumnVector(this.size);
		for (int i = 0; i < this.size; ++i)
			result.vector[i] = this.vector[i] * scalar;

		return result;
	}

	/**
	 * Creates a new ColumnVector by rounding this vector to the specified number of decimal places.
	 *
	 * @param numPlaces The number of places to round.
	 *
	 * @return The new vector.
	 */
	public ColumnVector roundTo(final int numPlaces) {
		double multipler = Math.pow(10, numPlaces);
		final ColumnVector result = new ColumnVector(this.size);
		for (int i = 0; i < this.size; ++i)
			result.vector[i] = Math.round(this.vector[i] * multipler) / multipler;

		return result;
	}

	/**
	 * Creates a new ColumnVector by multiplying this vector by a matrix.
	 *
	 * @param matrix The matrix.
	 *
	 * @return The new vector.
	 */
	public ColumnVector multiply(final Matrix matrix) {
		final int columnCount = matrix.getColumnCount();
		if (this.size != columnCount)
			throw new IllegalArgumentException("vector size and matrix column count must be equal");

		final int rowCount = matrix.getRowCount();
		final ColumnVector result = new ColumnVector(rowCount);
		for (int i = 0; i < rowCount; ++i) {
			double sumProduct = 0.0;
			for (int j = 0; j < columnCount; ++j)
				sumProduct += matrix.getAt(i, j) * this.vector[j];

			result.vector[i] = sumProduct;
		}

		return result;
	}

	/**
	 * Calculates the Manhattan distance (L1-norm) between the specified vector and this vector.
	 *
	 * @param vector The specified vector.
	 *
	 * @return The Manhattan distance (L1-norm).
	 */
	public double l1Distance(final ColumnVector vector) {
//		return this.distance(vector, d -> d); //TODO: L1 norm requires abs val
		return this.distance(vector, d -> Math.abs(d));
	}
	
	/**
	 * Calculates the Euclidean distance (L2-norm) between the specified vector and this vector.
	 *
	 * @param vector The specified vector.
	 * @return The Euclidean distance.
	 */
	public double l2Distance(final ColumnVector vector) {
		double distance = this.distance(vector, d -> d * d);
		return Math.sqrt(distance);
	}

	private double distance(final ColumnVector vector, final DoubleFunction<Double> aggregate) {
		if (this.size != vector.size)
			throw new IllegalArgumentException("cannot determine the distance between vectors with different sizes");

		double distance = 0;
		for (int i = 0; i < this.size; ++i) {
			double difference = this.vector[i] - vector.vector[i];
			distance += aggregate.apply(difference);
		}

		return distance;
	}
	
	@Override
	public ColumnVector clone() throws CloneNotSupportedException {
		super.clone();
		double [] clonedVector = new double[this.vector.length];
		System.arraycopy(this.vector, 0, clonedVector, 0, this.vector.length);
		return new ColumnVector(clonedVector);
	}

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < this.size; ++i) {
			if (0 != i)
				builder.append(" ");

			builder.append(format.format(this.vector[i]));
		}

		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.vector);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ColumnVector))
			return false;

		final ColumnVector rhs = (ColumnVector)obj;
		return Arrays.equals(this.vector, rhs.vector);
	}
}