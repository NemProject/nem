package org.nem.core.math;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public abstract class MatrixTest {

	//region abstract methods

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

	//endregion

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

	//region rowSum / columnSum

	@Test
	public void matrixRowSumsCanBeCalculated() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 11, 3, 1, 5, 6 });

		// Assert:
		Assert.assertThat(matrix.getRowSumVector(), IsEqual.equalTo(new ColumnVector(13.0, 4.0, 11.0)));
	}

	@Test
	public void matrixColumnSumsCanBeCalculated() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 11, 3, 1, 5, 6 });

		// Assert:
		Assert.assertThat(matrix.getColumnSumVector(), IsEqual.equalTo(new ColumnVector(10.0, 18.0)));
	}

	//endregion

	//region transpose

	@Test
	public void denseMatrixCanBeTransposed() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 7, 5, 1, 3, 11, 9 });

		// Act:
		final Matrix transposedMatrix = matrix.transpose();

		// Assert:
		Assert.assertThat(
				transposedMatrix,
				IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 7, 1, 11, 5, 3, 9 })));
	}

	@Test
	public void sparseMatrixCanBeTransposed() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, 5);

		// Act:
		final Matrix transposedMatrix = matrix.transpose();

		// Assert:
		Assert.assertThat(
				transposedMatrix,
				IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 0, 7, 0, 0, 0, 5 })));
	}

	//endregion

	//region normalizeColumns

	@Test
	public void allDenseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 11, 3, 1, 5, 8 });

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.2, 0.55, 0.3, 0.05, 0.5, 0.4 })));
	}

	@Test
	public void zeroMatrixCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3);

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 0, 0, 0, 0, 0, 0 })));
	}

	@Test
	public void matrixWithZeroSumColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 2, 0, 0, 0, -2, 0 });

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 1, 0, 0, 0, -1, 0 })));
	}

	@Test
	public void allSparseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(4, 2);
		matrix.setAt(0, 1, 4);
		matrix.setAt(2, 1, 12);

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(this.createMatrix(4, 2, new double[] { 0, 0.25, 0, 0, 0, 0.75, 0, 0 })));
	}

	//endregion

	//region absSum / sum

	@Test
	public void matrixAbsSumCanBeCalculatedForDenseMatrix() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, -3, -5, 11, -1, 8 });

		// Assert:
		Assert.assertThat(matrix.absSum(), IsEqual.equalTo(30.0));
	}

	@Test
	public void matrixAbsSumCanBeCalculatedForSparseMatrix() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, -5);

		// Assert:
		Assert.assertThat(matrix.absSum(), IsEqual.equalTo(12.0));
	}

	@Test
	public void matrixSumCanBeCalculatedForDenseMatrix() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, -3, -5, 11, -1, 8 });

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(12.0));
	}

	@Test
	public void matrixSumCanBeCalculatedForSparseMatrix() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, -5);

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(2.0));
	}

	//endregion

	//region scale

	@Test
	public void denseMatrixCanBeScaled() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });

		// Act:
		matrix.scale(10);

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.2, 0.3, 0.5, 1.1, 0.1, 0.8 })));
	}

	@Test
	public void sparseMatrixCanBeScaled() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(2, 0, 3);
		matrix.setAt(1, 1, 5);

		// Act:
		matrix.scale(5);

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.0, 0.0, 0.0, 1.0, 0.6, 0.0 })));
	}

	//endregion

	//region addElementWise

	@Test
	public void matrixCannotBeAddedWithDifferentSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2);
		final Matrix matrix2 = this.createMatrix(2, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> matrix1.addElementWise(matrix2), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> matrix2.addElementWise(matrix1), IllegalArgumentException.class);
	}

	@Test
	public void denseMatrixCanBeAddedWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });
		final Matrix matrix2 = this.createMatrix(3, 2, new double[] { 7, 3, 1, 5, 11, 9 });

		// Act:
		final Matrix result = matrix1.addElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 9, 6, 6, 16, 12, 17 })));
	}

	@Test
	public void sparseMatrixCanBeAddedWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2, new double[] { 1, 0, 2, 3, 0, 5 });

		final Matrix matrix2 = this.createMatrix(3, 2);
		matrix2.setAt(2, 0, 7);
		matrix2.setAt(1, 1, 5);

		// Act:
		final Matrix result = matrix1.addElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 1, 0, 2, 8, 7, 5 })));
	}

	//endregion

	//region multiplyElementWise

	@Test
	public void matrixCannotBeMultipliedElementWiseWithDifferentSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2);
		final Matrix matrix2 = this.createMatrix(2, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> matrix1.multiplyElementWise(matrix2), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> matrix2.multiplyElementWise(matrix1), IllegalArgumentException.class);
	}

	@Test
	public void denseMatrixCanBeMultipliedElementWiseWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });
		final Matrix matrix2 = this.createMatrix(3, 2, new double[] { 7, 3, 1, 5, 11, 9 });

		// Act:
		final Matrix result = matrix1.multiplyElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 14, 9, 5, 55, 11, 72 })));
	}

	@Test
	public void sparseMatrixCanBeMultipliedElementWiseWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(3, 2);
		matrix1.setAt(1, 1, 3);
		matrix1.setAt(0, 0, 7);

		final Matrix matrix2 = this.createMatrix(3, 2);
		matrix2.setAt(1, 1, 5);
		matrix1.setAt(2, 1, 8);

		// Act:
		final Matrix result = matrix1.multiplyElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0, 0, 0, 15, 0, 0 })));
	}

	//endregion
}
