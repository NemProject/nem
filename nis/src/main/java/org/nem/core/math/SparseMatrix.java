package org.nem.core.math;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents a linear algebra matrix which is sparsely populated.
 */
public class SparseMatrix {

	final int numRows;
	final int numCols;

	/**
	 * The rows of the matrix
	 */
	final private HashMap<Long, Double> entries;

	/**
	 * Creates a new matrix of the specified size which has a given capacity for each row.
	 *
	 * @param numRows The desired number of rows to represent.
	 * @param numCols The desired number of columns to represen.
	 * @param initialCapacity The initial of the hash map. Choose carefully to avoid rehashing!
	 */
	public SparseMatrix(final int numRows, final int numCols, final int initialCapacity) {
		this.numRows = numRows;
		this.numCols = numCols;
		entries = new HashMap<Long, Double>(initialCapacity);
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
	public double getAt(final long row, final long col) {
		if (row < 0 || row >= numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= numCols) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		long index = (row << 32) + col;
		Double value = this.entries.get(index);
		return value == null? 0 : value;
	}

	/**
	 * Sets a value at the specified row and column.
	 *
	 * @param row The row.
	 * @param col The column.
	 * @param val The value.
	 */
	public void setAt(final long row, final long col, final double value) {
		if (row < 0 || row >= numRows) {
			throw new IllegalArgumentException("Row index out of bounds");
		}
		if (col < 0 || col >= numCols) {
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
	 * Increments a value at the specified row and column by the given val.
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
		ColumnVector vector = new ColumnVector(numRows);
		for (Map.Entry<Long, Double> entry : this.entries.entrySet()) {
			vector.incrementAt((int)(entry.getKey() & 0xffffffff), Math.abs(entry.getValue()));
		}
		for (Map.Entry<Long, Double> entry : this.entries.entrySet()) {
			if (entry.getValue() != 0.0) {
				this.entries.put(entry.getKey(), entry.getValue()/vector.getAt((int)(entry.getKey() & 0xffffffff)));
			}
		}
	}

	/**
	 * Sum of all the matrix's elements in each row thus forming a vector
	 *
	 * @return The row sum vector.
	 */
	public ColumnVector getRowSumVector() {
		ColumnVector result = new ColumnVector(numRows);
		for (Map.Entry<Long, Double> entry : this.entries.entrySet()) {
			result.incrementAt((int)(entry.getKey() >> 32), entry.getValue());
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
	public ColumnVector multiply(final ColumnVector vector) {
		if (this.numCols != vector.size()) {
			throw new IllegalArgumentException("vector size and matrix column count must be equal");
		}
		
		final ColumnVector result = new ColumnVector(this.numRows);
		for (Map.Entry<Long, Double> entry : this.entries.entrySet()) {
			result.incrementAt((int)(entry.getKey() >> 32), entry.getValue() * vector.getAt((int)(entry.getKey() & 0xffffffff)));
		}
		return result;
	}	
}
