package org.nem.core.math;

public class SparseMatrixNonZeroElementRowIteratorTest extends MatrixNonZeroElementRowIteratorTest<SparseMatrix> {

	protected SparseMatrix createTestMatrix() {
		final SparseMatrix matrix = new SparseMatrix(4, 4, 4);
		matrix.setAtUnchecked(0, 0, 1.1);
		matrix.setAtUnchecked(0, 1, 2.2);
		matrix.setAtUnchecked(0, 3, 4.4);

		return matrix;
	}
}
