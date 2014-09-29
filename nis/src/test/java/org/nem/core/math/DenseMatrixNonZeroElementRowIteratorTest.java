package org.nem.core.math;

public class DenseMatrixNonZeroElementRowIteratorTest extends MatrixNonZeroElementRowIteratorTest<DenseMatrix> {

	protected DenseMatrix createTestMatrix() {
		final DenseMatrix matrix = new DenseMatrix(4, 4);
		matrix.setAtUnchecked(0, 0, 1.1);
		matrix.setAtUnchecked(0, 1, 2.2);
		matrix.setAtUnchecked(0, 3, 4.4);

		return matrix;
	}
}
