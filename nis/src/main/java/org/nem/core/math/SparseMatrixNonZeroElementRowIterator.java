package org.nem.core.math;

/**
 * Iterator to iterate through the non zero entries of a sparse matrix row very fast.
 */
public class SparseMatrixNonZeroElementRowIterator implements MatrixNonZeroElementRowIterator {
	private final int row;
	private final double[] values;
	private final int[] cols;
	private final int maxIndex;
	private int index = 0;

	public SparseMatrixNonZeroElementRowIterator(
			final int row,
			final double[] values,
			final int[] cols,
			final int maxIndex) {
		this.row = row;
		this.values = values;
		this.cols = cols;
		this.maxIndex = maxIndex;
	}

	@Override
	public boolean hasNext() {
		return index < maxIndex;
	}

	@Override
	public MatrixElement next() {
		return new MatrixElement(row, cols[index], values[index++]);
	}
}
