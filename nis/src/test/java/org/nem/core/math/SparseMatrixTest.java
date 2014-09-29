package org.nem.core.math;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class SparseMatrixTest extends MatrixTest<SparseMatrix> {

	//region forEach

	@Test
	public void forEachReturnsAllElements() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 0, 0, 1, -5, 8 });

		// Act:
		final List<Double> values = new ArrayList<>();
		matrix.forEach((row, col, value) -> values.add(value));

		// Assert: zero-values are excluded
		Assert.assertThat(values, IsEquivalent.equivalentTo(new Double[] { 2.0, 1.0, -5.0, 8.0 }));
	}

	//endregion

	//region toString

	@Test
	public void sparseMatrixStringRepresentationIsCorrect() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] {
				2.1234, 11.1234, 0, 1, 5012.0126, 0
		});

		// Assert:
		final String expectedString =
				"[3 x 2]" + System.lineSeparator()
						+ "(0, 0) -> 2.123" + System.lineSeparator()
						+ "(0, 1) -> 11.123" + System.lineSeparator()
						+ "(1, 1) -> 1.000" + System.lineSeparator()
						+ "(2, 0) -> 5012.013";
		Assert.assertThat(matrix.toString(), IsEqual.equalTo(expectedString));
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
		Assert.assertThat(
				sparseMatrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 2, 0, 5, 11, 1, 8 })));
	}

	@Test
	public void entryCanBeRemovedWhenValuesAreAddedOutOfOrder() {
		// Arrange:
		final SparseMatrix sparseMatrix = this.createMatrix(3, 2);
		final int numRows = 3;
		final int numCols = 2;
		final double[] values = new double[] { 2, 3, 5, 11, 1, 8 };
		for (int r = 0; r < numRows; ++r) {
			for (int c = 0; c < numCols; ++c) {
				final int r2 = numRows - r - 1;
				final int c2 = numCols - c - 1;
				sparseMatrix.setAt(r2, c2, values[r2 * numCols + c2]);
			}
		}

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(2));

		// Act:
		sparseMatrix.setAt(0, 1, 0.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(1));
		Assert.assertThat(
				sparseMatrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 2, 0, 5, 11, 1, 8 })));
	}

	@Test
	public void lastEntryInRowCanBeRemoved() {
		// Arrange:
		final SparseMatrix sparseMatrix = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(2));

		// Act:
		sparseMatrix.setAt(0, 1, 0.0);
		sparseMatrix.setAt(0, 0, 0.0);

		// Assert:
		Assert.assertThat(sparseMatrix.getNonZeroColumnCount(0), IsEqual.equalTo(0));
		Assert.assertThat(
				sparseMatrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0, 0, 5, 11, 1, 8 })));
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
	
	//region numEntries
	@Test
	public void numEntriesReturnsCorrectNumber() {
		// Arrange:
		final SparseMatrix sparseMatrix1 = new SparseMatrix(3, 3, 1);
		
		final SparseMatrix sparseMatrix2 = new SparseMatrix(3, 3, 1);
		sparseMatrix2.setAt(0, 0, 5.0);
		
		final SparseMatrix sparseMatrix3 = new SparseMatrix(5, 5, 1);
		sparseMatrix3.setAt(0, 0, 5.0);
		sparseMatrix3.setAt(3, 3, -5.0);

		// Act and Assert:
		Assert.assertThat(sparseMatrix1.getNumEntries(), IsEqual.equalTo(0));
		Assert.assertThat(sparseMatrix2.getNumEntries(), IsEqual.equalTo(1));
		Assert.assertThat(sparseMatrix3.getNumEntries(), IsEqual.equalTo(2));
	}
	//endregion
	
	//region add
	
	@Test
	public void sparseMatrixCanBeAddedToByScalar() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(2, 0, 3);
		matrix.setAt(1, 1, 5);

		// Act::
		final Matrix result = matrix.add(0.2);

		// Assert:
		Assert.assertThat(
				result,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.0, 0.0, 0.0, 5.2, 3.2, 0.0 })));
	}
		
	// endregion

	// region sorted columns

	@Test
	public void setAtUncheckedKeepsColumnsSorted() {
		// Arrange:
		final SparseMatrix sparseMatrix = new SparseMatrix(8, 8, 8);

		// Act:
		sparseMatrix.setAt(0, 7, 5.0);
		sparseMatrix.setAt(0, 1, 5.0);
		sparseMatrix.setAt(0, 0, 5.0);
		sparseMatrix.setAt(0, 5, 5.0);
		sparseMatrix.setAt(0, 4, 5.0);

		// Assert:
		int col = -1;
		MatrixNonZeroElementRowIterator iterator = sparseMatrix.getNonZeroElementRowIterator(0);
		while (iterator.hasNext()) {
			MatrixElement entry = iterator.next();
			Assert.assertThat(entry.getColumn() > col, IsEqual.equalTo(true));
			col = entry.getColumn();
		}
	}

	// endregion

	@Override
	protected SparseMatrix createMatrix(final int rows, final int cols) {
		return new SparseMatrix(rows, cols, 100);
	}
}
