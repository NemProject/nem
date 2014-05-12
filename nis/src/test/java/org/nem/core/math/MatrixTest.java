package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;

public class MatrixTest {

	//region constructor / getAt / setAt

	@Test
	public void matrixIsInitializedToZero() {
		// Arrange:
		final Matrix matrix = new Matrix(2, 3);

		// Assert:
		Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void matrixValuesCanBeSet() {
		// Arrange:
		final Matrix matrix = new Matrix(3, 2);

		// Act:
		matrix.setAt(0, 0, 7);
		matrix.setAt(0, 1, 3);
		matrix.setAt(1, 0, 5);
		matrix.setAt(1, 1, 11);
		matrix.setAt(2, 0, 1);
		matrix.setAt(2, 1, 9);

		// Assert:
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(3.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(1.0));
		Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(9.0));
	}
	
	@Test
	public void matrixValuesCanBeIncremented() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				7, 3, 5, 11, 1, 9
		});

		// Act:
		// Increment values
		matrix.incrementAt(0, 0, 1);
		matrix.incrementAt(1, 0, 2);
		matrix.incrementAt(2, 0, 4);
		matrix.incrementAt(0, 1, 4);
		matrix.incrementAt(1, 1, 5);
		matrix.incrementAt(2, 1, 6);
		
		// Double increment
		matrix.incrementAt(2, 1, 7);

		// Assert:
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(8.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(9.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(15.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(6.0));
		Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(22.0));
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

	private static void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final Matrix matrix = new Matrix(numRows, numCols);

			// Act:
			matrix.getAt(row, col);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
	}

	private static void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		try {
			// Arrange:
			final Matrix matrix = new Matrix(numRows, numCols);

			// Act:
			matrix.setAt(row, col, 0);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
	}

	//endregion

	//region transpose

	@Test
	public void matrixCanBeTransposed() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				7, 5, 1, 3, 11, 9
		});

		// Act:
		final Matrix transposedMatrix = matrix.transpose();

		// Assert:
		Assert.assertThat(transposedMatrix.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(transposedMatrix.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(transposedMatrix.getAt(0, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(transposedMatrix.getAt(0, 1), IsEqual.equalTo(5.0));
		Assert.assertThat(transposedMatrix.getAt(0, 2), IsEqual.equalTo(1.0));
		Assert.assertThat(transposedMatrix.getAt(1, 0), IsEqual.equalTo(3.0));
		Assert.assertThat(transposedMatrix.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(transposedMatrix.getAt(1, 2), IsEqual.equalTo(9.0));
	}

	//endregion

	//region normalizeColumns

	@Test
	public void allMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2, 3, 5, 11, 1, 8
		});

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.2));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.3));
		Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(0.5));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.55));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.05));
		Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(0.4));
	}

	//endregion

	//region absSum / sum

	@Test
	public void matrixAbsSumCanBeCalculated() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2, -3, -5, 11, -1, 8
		});

		// Assert:
		Assert.assertThat(matrix.absSum(), IsEqual.equalTo(30.0));
	}

	@Test
	public void matrixSumCanBeCalculated() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2, -3, -5, 11, -1, 8
		});

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(12.0));
	}

	//endregion

	//region multiplyElementWise

	@Test(expected = IllegalArgumentException.class)
	public void matrixCannotBeMultipliedElementWiseWithDifferentSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = new Matrix(3, 2);
		final Matrix matrix2 = new Matrix(2, 3);

		// Act:
		matrix1.multiplyElementWise(matrix2);
	}

	@Test
	public void matrixCanBeMultipliedElementWiseWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = createThreeByTwoMatrix(new double[] {
				2, 3, 5, 11, 1, 8
		});

		final Matrix matrix2 = createThreeByTwoMatrix(new double[] {
				7, 3, 1, 5, 11, 9
		});

		// Act:
		final Matrix result = matrix1.multiplyElementWise(matrix2);

		// Assert:
		Assert.assertThat(result.getAt(0, 0), IsEqual.equalTo(14.0));
		Assert.assertThat(result.getAt(1, 0), IsEqual.equalTo(9.0));
		Assert.assertThat(result.getAt(2, 0), IsEqual.equalTo(5.0));
		Assert.assertThat(result.getAt(0, 1), IsEqual.equalTo(55.0));
		Assert.assertThat(result.getAt(1, 1), IsEqual.equalTo(11.0));
		Assert.assertThat(result.getAt(2, 1), IsEqual.equalTo(72.0));
	}

	//endregion

	//region rowSum / columnSum

	@Test
	public void matrixRowSumsCanBeCalculated() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2, 3, 5, 11, 1, 6
		});

		// Assert:
		Assert.assertThat(matrix.rowSum(0), IsEqual.equalTo(13.0));
		Assert.assertThat(matrix.rowSum(1), IsEqual.equalTo(4.0));
		Assert.assertThat(matrix.rowSum(2), IsEqual.equalTo(11.0));
	}

	@Test
	public void matrixColumnSumsCanBeCalculated() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2, 3, 5, 11, 1, 6
		});

		// Assert:
		Assert.assertThat(matrix.columnSum(0), IsEqual.equalTo(10.0));
		Assert.assertThat(matrix.columnSum(1), IsEqual.equalTo(18.0));
	}

	//endregion

	//region isSameSize

	@Test
	public void isSameSizeReturnsTrueWhenMatriciesHaveSameSize() {
		// Arrange:
		final Matrix matrix = new Matrix(3, 2);

		// Assert:
		Assert.assertThat(matrix.isSameSize(new Matrix(3, 2)), IsEqual.equalTo(true));
		Assert.assertThat(matrix.isSameSize(new Matrix(2, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new Matrix(4, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new Matrix(3, 1)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new Matrix(3, 3)), IsEqual.equalTo(false));

	}

	//endregion

	//region toString

	@Test
	public void matrixStringRepresentationIsCorrect() {
		// Arrange:
		final Matrix matrix = createThreeByTwoMatrix(new double[] {
				2.1234, 3.2345, 5012.0126, 11.1234, 1, 8
		});

		// Assert:
		final String expectedResult =
				"2.123 11.123" + System.lineSeparator() +
						"3.235 1.000" + System.lineSeparator() +
						"5012.013 8.000";

		// Assert:
		Assert.assertThat(matrix.toString(), IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static Matrix createThreeByTwoMatrix(final double[] values) {
		if (6 != values.length)
			throw new IllegalArgumentException("values must have 6 elements");

		// Arrange:
		final Matrix matrix = new Matrix(3, 2);
		matrix.setAt(0, 0, values[0]);
		matrix.setAt(1, 0, values[1]);
		matrix.setAt(2, 0, values[2]);
		matrix.setAt(0, 1, values[3]);
		matrix.setAt(1, 1, values[4]);
		matrix.setAt(2, 1, values[5]);
		return matrix;
	}
}
