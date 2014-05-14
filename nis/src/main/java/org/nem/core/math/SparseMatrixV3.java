package org.nem.core.math;

import gnu.trove.iterator.TLongDoubleIterator;
import gnu.trove.map.hash.TLongDoubleHashMap;

public class SparseMatrixV3 {

	final int numRows;
	final int numCols;

	/**
	 * The rows of the matrix
	 */
	final private TLongDoubleHashMap entries;
	private double[] values=null;
	private long[] indices=null;
	private boolean converted = false;

	/**
	 * Creates a new matrix of the specified size which has a given capacity for each row.
	 *
	 * @param numRows The desired number of rows to represent.
	 * @param numCols The desired number of columns to represent.
	 * @param initialCapacity The initial of the hash map. Choose carefully to avoid rehashing!
	 */
	public SparseMatrixV3(final int numRows, final int numCols, final int initialCapacity) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.entries = new TLongDoubleHashMap(initialCapacity);
	}
	
	/**
	 * Converts the hash map into 2 arrays for faster operations.
	 */
	public void convert() {
		this.values = new double[this.entries.size()];
		this.indices = new long[this.entries.size()];
		int i=0;
		for ( TLongDoubleIterator it = entries.iterator(); it.hasNext(); ) {
		    it.advance();
		    this.values[i] = it.value();
		    this.indices[i++] = it.key();
		}
		this.converted = true;
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
		return this.entries.size();
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
		double value = this.entries.get(index);
		return value;
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
		if (value == 0.0) {
			this.entries.remove(index);
		} else {
			this.entries.put(index, value);
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
		if (row < 0 || row >= this.numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= this.numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		long index = (row << 32) + col;
		double value = getAt(row, col) + val;
		if (value == 0.0) {
			this.entries.remove(index);
		} else {
			this.entries.put(index, value);
		}
	}

	/**
	 * Normalizes each column of the matrix.
	 */
	public void normalizeColumns() {
		double[] vector = new double[this.numRows];
		if (this.converted) {
			int arraySize = this.indices.length;
			for (int i=0; i<arraySize; i++) {
				long index = this.indices[i];
				vector[(int)(index & 0xffffffff)] +=  Math.abs(this.values[i]);
			}			
			for (int i=0; i<arraySize; i++) {
				long index = this.indices[i];
				double value = this.values[i];
				if (value != 0.0) {
					this.values[i] /= vector[(int)(index & 0xffffffff)];
				}
			}
		} else {
			for ( TLongDoubleIterator it = this.entries.iterator(); it.hasNext(); ) {
			    it.advance();
				vector[(int)(it.key() & 0xffffffff)] += Math.abs(it.value());
			}
			for ( TLongDoubleIterator it = this.entries.iterator(); it.hasNext(); ) {
			    it.advance();
			    double value = it.value();
				if (value != 0.0) {
					this.entries.put(it.key(), value/vector[(int)(it.key() & 0xffffffff)]);
				}
			}
		}
	}

	/**
	 * Sum of all the matrix's elements in each row thus forming a vector
	 *
	 * @return The row sum vector.
	 */
	public ColumnVector getRowSumVector() {
		double[] result = new double[this.numRows];
		if (this.converted) {
			int arraySize = this.indices.length;
			for (int i=0; i<arraySize; i++) {
				long index = this.indices[i];
				double value = this.values[i];
				result[(int)(index >> 32)] += value;
			}
		} else {
			for ( TLongDoubleIterator it = this.entries.iterator(); it.hasNext(); ) {
			    it.advance();
				result[(int)(it.key() >> 32)] += it.value();
			}
		}
		return new ColumnVector(result);
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
		double[] result = new double[this.numRows];
		double[] rawVector = vector.getVector(); 
		if (this.converted) {
			int arraySize = this.indices.length;
			for (int i=0; i<arraySize; i++) {
				long index = this.indices[i];
				result[(int)(index >> 32)] += this.values[i] * rawVector[(int)(index & 0xffffffff)];
			}
		} else {
			for ( TLongDoubleIterator it = this.entries.iterator(); it.hasNext(); ) {
			    it.advance();
			    long index = it.key();
				result[(int)(index >> 32)] += it.value() * rawVector[(int)(index & 0xffffffff)];
			}
		}
		return new ColumnVector(result);
	}	
}
