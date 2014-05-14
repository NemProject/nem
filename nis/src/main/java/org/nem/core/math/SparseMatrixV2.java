package org.nem.core.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SparseMatrixV2 {

	final int numRows;
	final int numCols;

	/**
	 * The rows of the matrix
	 */
	private ArrayList<Double> values=null;
	private ArrayList<Long> indices=null;
	private double[] rawValues=null;
	private long[] rawIndices=null;
	private boolean converted=false;

	/**
	 * Creates a new matrix of the specified size which has a given capacity.
	 *
	 * @param numRows The desired number of rows to represent.
	 * @param numCols The desired number of columns to represent.
	 * @param initialCapacity The initial of the array. Choose carefully to avoid reallocation!
	 */
	public SparseMatrixV2(final int numRows, final int numCols, final int initialCapacity) {
		this.numRows = numRows;
		this.numCols = numCols;
		values = new ArrayList<Double>(initialCapacity);
		indices = new ArrayList<Long>(initialCapacity);
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
	 * Gets the number of non-zero entries.
	 *
	 * @return The number of non-zero entries.
	 */
	public int getEntryCount() {
		return this.values.size();
	}

	/**
	 * Gets the value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 *
	 * @return The value.
	 */
	public double getAt(final long row, final long col) {
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		long index = (row << 32) + col;
		int idx = Collections.binarySearch(this.indices, index);
		if (idx < 0) {
			return 0.0;
		}
		return this.values.get(idx);
	}

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param value The value.
	 */
	public void setAt(final long row, final long col, final double value) {
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		long index = (row << 32) + col;
		int idx = Collections.binarySearch(this.indices, index);
		if (value == 0.0) {
			if (idx >= 0) {
				this.values.remove(index);
				this.indices.remove(index);
			}
		} else {
			if (idx < 0) {
				this.values.add(-idx-1, value);
				this.indices.add(-idx-1, index);
			} else {
				this.values.set(idx, this.values.get(idx) + value);				
			}
		}
	}

	/**
	 * Increments a value at the specified row and column by the given value.
	 * 
	 * @param row The row.
	 * @param col The column.
	 * @param val The value to increment by.
	 */
	public void incrementAt(final long row, final long col, final double val) {
		if (row < 0 || row >= numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		long index = (row << 32) + col;
		double value = getAt(row, col) + val;
		int idx = Collections.binarySearch(this.indices, index);
		if (value == 0.0) {
			if (idx >= 0) {
				this.values.remove(index);
				this.indices.remove(index);
			}
		} else {
			if (idx < 0) {
				this.values.add(-idx-1, value);
				this.indices.add(-idx-1, index);
			} else {
				this.values.set(idx, value);				
			}
		}
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeColumns() {
		double[] vector = new double[this.numRows];
		int size = this.indices.size();
		for (int i=0; i<size; i++) {
			long index = this.indices.get(i);
			vector[(int)(index & 0xffffffff)] += Math.abs(this.values.get(i));
		}
		for (int i=0; i<size; i++) {
			long index = this.indices.get(i);
			double val = vector[(int)(index & 0xffffffff)];
			if (val != 0.0) {
				this.values.set(i, this.values.get(i)/val);
			}
		}
	}

	/**
	 * Sum of all the matrix's elements in each row thus forming a vector
	 *
	 * @return The row sum vector.
	 */
	public double[] getRowSumVector() {
		double[] result = new double[this.numRows];
		int size = this.indices.size();
		for (int i=0; i<size; i++) {
			long index = this.indices.get(i);
			result[(int)(index >> 32)] += this.values.get(i);
		}
		return result;
	}

	/**
	 * Multiplies this sparse matrix by a vector.
	 *
	 * @param vector The vector.
	 *
	 * @return The resulting vector.
	 */
	public double[] multiply(final ColumnVector vector) {
		if (this.numCols != vector.size()) {
			throw new IllegalArgumentException("vector size and matrix column count must be equal");
		}
		double[] result = new double[this.numRows];
		double[] rawVector = vector.getVector(); 
		if (this.converted) {
//			int arraySize = indices.length;
//			for (int i=0; i<arraySize; i++) {
//				long index = indices[i];
//				result[(int)(index >> 32)] += this.values[i] * rawVector[(int)(index & 0xffffffff)];
//			}
		} else {
			int size = this.indices.size();
			for (int i=0; i<size; i++) {
				long index = this.indices.get(i);
				result[(int)(index >> 32)] += this.values.get(i) * rawVector[(int)(index & 0xffffffff)];
			}
		}
		return result;
	}	
}
