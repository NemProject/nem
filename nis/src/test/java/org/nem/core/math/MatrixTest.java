package org.nem.core.math;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public abstract class MatrixTest {

	//region getAt / setAt / incrementAt

	@Test
	public void matrixValuesCanBeSet() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);

		// Act:
		matrix.setAt(0, 0, 7);
		matrix.setAt(0, 1, 3);
		matrix.setAt(1, 0, 5);
		matrix.setAt(1, 1, 11);
		matrix.setAt(2, 0, 0);
		matrix.setAt(2, 1, 9);

		// Assert:
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(3.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(9.0));
	}

	@Test
	public void matrixGetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertGetOutOfBounds(2, 3, -1, 0);
		assertGetOutOfBounds(2, 3, 0, -1);
		assertGetOutOfBounds(2, 3, 2, 0);
		assertGetOutOfBounds(2, 3, 0, 3);
	}

	@Test
	public void matrixSetCannotBeIndexedOutOfBounds() {
		// Assert:
		assertSetOutOfBounds(2, 3, -1, 0);
		assertSetOutOfBounds(2, 3, 0, -1);
		assertSetOutOfBounds(2, 3, 2, 0);
		assertSetOutOfBounds(2, 3, 0, 3);
	}

	private void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		// Assert:
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final Matrix matrix = this.createMatrix(numRows, numCols);

			// Act:
			matrix.getAt(row, col);
		}, IndexOutOfBoundsException.class);
	}

	private void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final Matrix matrix = this.createMatrix(numRows, numCols);

			// Act:
			matrix.setAt(row, col, 0.0);
		}, IndexOutOfBoundsException.class);
	}

	@Test
	public void matrixValuesCanBeIncremented() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 1, 4, 5, 7, 2, 11 });

		// Act:
		// Increment values
		matrix.incrementAt(0, 0, 4);
		matrix.incrementAt(0, 1, 2);
		matrix.incrementAt(0, 2, 3);
		matrix.incrementAt(1, 0, 4);
		matrix.incrementAt(1, 1, 5);
		matrix.incrementAt(1, 2, 6);

		// Double increment
		matrix.incrementAt(0, 2, 7);

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 5, 6, 15, 11, 7, 17 })));
	}

	//endregion

	//region isSameSize

	@Test
	public void isSameSizeReturnsTrueWhenMatriciesHaveSameSize() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);

		// Assert:
		Assert.assertThat(matrix.isSameSize(this.createMatrix(3, 2)), IsEqual.equalTo(true));
		Assert.assertThat(matrix.isSameSize(this.createMatrix(2, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(this.createMatrix(4, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(this.createMatrix(3, 1)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(this.createMatrix(3, 3)), IsEqual.equalTo(false));
	}

	//endregion

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	protected abstract Matrix createMatrix(final int rows, final int cols);

	/**
	 * Creates a new matrix of the specified size and initial values.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 * @param values The initial values.
	 */
	protected abstract Matrix createMatrix(final int rows, final int cols, final double[] values);
}
