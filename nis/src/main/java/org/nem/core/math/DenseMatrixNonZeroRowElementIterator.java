package org.nem.core.math;

/**
 * Iterator to iterate through the non zero entries of a dense matrix.
 */
public class DenseMatrixNonZeroRowElementIterator implements MatrixNonZeroElementRowIterator {
	private final int row;
	private final DenseMatrix matrix;
	private int index = 0;

	public DenseMatrixNonZeroRowElementIterator(
			final int row,
			final DenseMatrix matrix) {
		this.row = row;
		this.matrix = matrix;
	}

	@Override
	public boolean hasNext() {
		for (int i=index; i<matrix.getColumnCount(); i++) {
			if (matrix.getAt(row, i) != 0.0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MatrixElement next() {
		while (index < matrix.getColumnCount()) {
			if (matrix.getAt(row, index++) != 0.0) {
				return new MatrixElement(row, index - 1, matrix.getAt(row, index - 1));
			}
		}
		throw new RuntimeException("index out of range");
	}
}
