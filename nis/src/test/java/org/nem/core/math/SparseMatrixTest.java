package org.nem.core.math;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class SparseMatrixTest extends MatrixTest<SparseMatrix> {

	//region toString

	@Test
	public void sparseMatrixStringRepresentationIsCorrect() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] {
				2.1234, 11.1234, 3.2345, 1, 5012.0126, 8
		});

		// Assert:
		Assert.assertThat(matrix.toString(), IsEqual.equalTo("[3 x 2]"));
	}

	//endregion

	//region removal / reallocation

	@Test
	public void entryCanBeRemoved() {
		// Arrange:
		final SparseMatrix sparseMatrix = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(2));

		// Act:
		sparseMatrix.setAt(0, 1, 0.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(1));
	}

	@Test
	public void rowCanBeReallocated() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(3, 2, 1);
		sparseMatrix.setAt(0, 0, 5.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getRowCapacity(0), IsEqual.equalTo(1));

		// Act:
		sparseMatrix.setAt(0, 1, 3.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getRowCapacity(0), IsEqual.equalTo(2));
		Assert.assertThat(sparseMatrix.getAt(0, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(sparseMatrix.getAt(0, 1), IsEqual.equalTo(3.0));
	}

	//endregion

	@Override
	protected SparseMatrix createMatrix(int rows, int cols) {
		return new SparseMatrix(rows, cols, 100);
	}
}
