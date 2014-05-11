package org.nem.core.math;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a sparsely populated row vector
 */
public class SparseRowVector {
	
	/**
	 * The length of the vector that this object represents
	 */
	private final int size;

	/**
	 * The hash map containing the column index to value mappings
	 */
	final private HashMap<Integer, Double> values;
	
	/**
	 * Creates a sparse row.
	 *
	 * @param size The size of the vector.
	 * @param initialCapacity The initial capacity for the hash map.
	 */
	public SparseRowVector(final int size, final int initialCapacity) {
		this.size = size;
		this.values = new HashMap<Integer, Double>(initialCapacity);
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
	 * Gets the size of the underlying hash map, i.e. the number of nonzero components.
	 *
	 * @return The number of nonzero components.
	 */
	public int compressedSize() {
		return this.values.size();
	}

	/**
	 * Gets the value at the specified index.
	 *
	 * @param index The index.
	 *
	 * @return The value.
	 */
	public Double getAt(int index) {
		Double value = this.values.get(index); 
		return value == null? 0.0 : value;
	}

	/**
	 * Sets a value at the specified index.
	 *
	 * @param index The index.
	 * @param val   The value.
	 */
	public void setAt(final int index, double val) {
		if (val == 0.0) {
			this.values.remove(index);
		} else {
			this.values.put(index, val);
		}
	}

	/**
	 * Increments a value at the specified index.
	 *
	 * @param index The index.
	 * @param val   The value.
	 */
	public void incrementAt(final int index, double val) {
		double value = getAt(index) + val;
		if (value == 0.0) {
			this.values.remove(index);
		} else {
			this.values.put(index, value);
		}
	}
	
	/**
	 * Gets the sum of all elements of this row.
	 *
	 * @return The sum of all elements of a row.
	 */
	public double sum() {
		double sum = 0.0;
		for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
			sum += entry.getValue();
		}
		
		return sum;
	}

	/**
	 * Gets the sum of all absolute values of this row.
	 *
	 * @return The sum of all elements of a row.
	 */
	public double absSum() {
		double sum = 0.0;
		for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
			sum += Math.abs(entry.getValue());
		}
		
		return sum;
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
		for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
			this.values.put(entry.getKey(), entry.getValue() / scale);
		}
	}

	/**
	 * Creates a new SparseRowVector by multiplying this vector by a scalar.
	 *
	 * @param scalar The scalar.
	 *
	 * @return The new vector.
	 */
	public SparseRowVector multiply(final double scalar) {
		final SparseRowVector result = new SparseRowVector(this.size, this.values.size() * 2);
		for (Map.Entry<Integer, Double> entry : this.values.entrySet()) {
			result.setAt(entry.getKey(), entry.getValue() * scalar);
		}
		
		return result;
	}

	/**
	 * Performs a scalar multiplication of this row vector and a column vector.
	 *
	 * @param vector The column vector.
	 *
	 * @return The scalar product.
	 */
	public double multiply(final ColumnVector vector) {
		if (this.size != vector.size()) {
			throw new IllegalArgumentException("row vector size and column vector size must be equal");
		}
		
		double result = 0.0;
		for (Map.Entry<Integer, Double> entry : values.entrySet()) {
			result += vector.getAt(entry.getKey()) * entry.getValue();
		}
		
		return result;
	}
}
