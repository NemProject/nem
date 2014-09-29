package org.nem.core.math;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public abstract class MatrixNonZeroElementRowIteratorTest<TMatrix extends Matrix> {

	@Test
	public void hasNextReturnsTrueIfMoreColumnsAreAvailable() {
		// Arrange:
		final TMatrix matrix = createTestMatrix();
		MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(0);

		// Act + Assert
		for (int i=0; i<3; i++) {
			Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
			iterator.next();
		}
	}

	@Test
	public void hasNextReturnsFalseIfNoMoreColumnsAreAvailable() {
		// Arrange:
		final TMatrix matrix = createTestMatrix();
		MatrixNonZeroElementRowIterator iterator1 = matrix.getNonZeroElementRowIterator(0);
		MatrixNonZeroElementRowIterator iterator2 = matrix.getNonZeroElementRowIterator(1);

		// Act:
		for (int i=0; i<3; i++) {
			iterator1.next();
		}

		// Assert:
		Assert.assertThat(iterator1.hasNext(), IsEqual.equalTo(false));
		Assert.assertThat(iterator2.hasNext(), IsEqual.equalTo(false));
	}

	@Test
	public void nextReturnsCorrectMatrixElement() {
		// Arrange:
		final TMatrix matrix = createTestMatrix();
		MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(0);

		// Act + Assert:
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 0, 1.1)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 1, 2.2)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 3, 4.4)));
	}

	protected abstract TMatrix createTestMatrix();
	/*{
		final TMatrix matrix = this.createMatrix(rows, cols);
		new SparseMatrix(4, 4, 4);
		sparseMatrix.setAtUnchecked(0, 0, 1.1);
		sparseMatrix.setAtUnchecked(0, 1, 2.2);
		sparseMatrix.setAtUnchecked(0, 3, 4.4);

		return sparseMatrix;
	}*/
}
