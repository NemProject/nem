package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DenseMatrixTest {

	//region constructor

	@Test
	public void matrixIsInitializedToZero() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3);

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
	public void matrixCanBeInitializedWithDefaultValues() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2, 3 });

		// Assert:
		Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(1.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(4.0));
		Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(5.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(2.0));
		Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(3.0));
	}

	@Test
	public void matrixCannotBeInitializedWithIncompleteDefaultValues() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2 }),
				IllegalArgumentException.class);
	}

	//endregion

	//region getAt / setAt / incrementAt

	@Test
	public void matrixValuesCanBeSet() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(3, 2);

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

	private static void assertGetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		// Assert:
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final Matrix matrix = new DenseMatrix(numRows, numCols);

			// Act:
			matrix.getAt(row, col);
		}, ArrayIndexOutOfBoundsException.class);
	}

	private static void assertSetOutOfBounds(final int numRows, int numCols, final int row, final int col) {
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final Matrix matrix = new DenseMatrix(numRows, numCols);

			// Act:
			matrix.setAt(row, col, 0.0);
		}, ArrayIndexOutOfBoundsException.class);
	}

	@Test
	public void matrixValuesCanBeIncremented() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2, 11 });

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
				IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 5, 6, 15, 11, 7, 17 })));
	}

	//endregion

	//region setAll

	@Test
	public void setAllSetsAllMatrixElementValuesToDefaultValue() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2, 3 });

		// Act:
		matrix.setAll(3.2);

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 3.2, 3.2, 3.2, 3.2, 3.2, 3.2 })));
	}

	@Test
	public void setAllSetsAllMatrixElementValuesToSpecificValues() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2, 3 });

		// Act:
		matrix.setAll(new double[] { 6, 8, 4, 3, 9, 11 });

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 6, 8, 4, 3, 9, 11 })));
	}

	@Test
	public void setAllCannotInitializeMatrixWithIncompleteDefaultValues() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new DenseMatrix(2, 3, new double[] { 1, 4, 5, 7, 2 }),
				IllegalArgumentException.class);
	}

	//endregion

	//region transpose

	@Test
	public void denseMatrixCanBeTransposed() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(3, 2, new double[] { 7, 5, 1, 3, 11, 9 });

		// Act:
		final Matrix transposedMatrix = matrix.transpose();

		// Assert:
		Assert.assertThat(
				transposedMatrix,
				IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 7, 1, 11, 5, 3, 9 })));
	}

	@Test
	public void sparseMatrixCanBeTransposed() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(3, 2);
		matrix.setAll(3);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, 5);

		// Act:
		final Matrix transposedMatrix = matrix.transpose();

		// Assert:
		Assert.assertThat(
				transposedMatrix,
				IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 3, 7, 3, 3, 3, 5 })));
	}

	//endregion

	//region normalizeColumns

	@Test
	public void allDenseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(3, 2, new double[] { 2, 11, 3, 1, 5, 8 });

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 0.2, 0.55, 0.3, 0.05, 0.5, 0.4 })));
	}

	@Test
	public void zeroMatrixCanBeNormalized() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3);

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 0, 0, 0, 0, 0, 0 })));
	}

	@Test
	public void matrixWithZeroSumColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[] { 2, 0, 0, 0, -2, 0 });

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(2, 3, new double[] { 1, 0, 0, 0, -1, 0 })));
	}

	@Test
	public void allSparseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(4, 2);
		matrix.setAt(0, 1, 4);
		matrix.setAt(2, 1, 12);

		// Act:
		matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(new DenseMatrix(4, 2, new double[] { 0, 0.25, 0, 0, 0, 0.75, 0, 0 })));
	}

	//endregion

	//region absSum / sum

	@Test
	public void matrixAbsSumCanBeCalculatedForDenseMatrix() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(3, 2, new double[] { 2, -3, -5, 11, -1, 8 });

		// Assert:
		Assert.assertThat(matrix.absSum(), IsEqual.equalTo(30.0));
	}

	@Test
	public void matrixAbsSumCanBeCalculatedForSparseMatrix() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(3, 2);
		matrix.setAll(3);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, -5);

		// Assert:
		Assert.assertThat(matrix.absSum(), IsEqual.equalTo(24.0));
	}

	@Test
	public void matrixSumCanBeCalculatedForDenseMatrix() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(3, 2, new double[] { 2, -3, -5, 11, -1, 8 });

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(12.0));
	}

	@Test
	public void matrixSumCanBeCalculatedForSparseMatrix() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(3, 2);
		matrix.setAll(3);
		matrix.setAt(1, 0, 7);
		matrix.setAt(2, 1, -5);

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(14.0));
	}

	//endregion

	//region add

	// TODO:
//	@Test
//	public void matrixCannotBeAddedWithDifferentSizeMatrix() {
//		// Arrange:
//		final Matrix matrix1 = new DenseMatrix(3, 2);
//		final Matrix matrix2 = new DenseMatrix(2, 3);
//
//		// Assert:
//		ExceptionAssert.assertThrows(v -> matrix1.add(matrix2), IllegalArgumentException.class);
//		ExceptionAssert.assertThrows(v -> matrix2.add(matrix1), IllegalArgumentException.class);
//	}
//
//	@Test
//	public void denseMatrixCanBeAddedWithSameSizeMatrix() {
//		// Arrange:
//		final Matrix matrix1 = new DenseMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });
//		final Matrix matrix2 = new DenseMatrix(3, 2, new double[] { 7, 3, 1, 5, 11, 9 });
//
//		// Act:
//		final Matrix result = matrix1.add(matrix2);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 9, 6, 6, 16, 12, 17 })));
//	}
//
//	@Test
//	public void sparseMatrixCanBeAddedWithSameSizeMatrix() {
//		// Arrange:
//		final DenseMatrix matrix1 = new DenseMatrix(3, 2, new double[] { 1, 0, 2, 3, 0, 5 });
//
//		final DenseMatrix matrix2 = new DenseMatrix(3, 2);
//		matrix2.setAll(2);
//		matrix2.setAt(1, 1, 5);
//
//		// Act:
//		final Matrix result = matrix1.add(matrix2);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 3, 2, 4, 8, 2, 7 })));
//	}

	//endregion

	//region multiplyElementWise

	@Test
	public void matrixCannotBeMultipliedElementWiseWithDifferentSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = new DenseMatrix(3, 2);
		final Matrix matrix2 = new DenseMatrix(2, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> matrix1.multiplyElementWise(matrix2), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> matrix2.multiplyElementWise(matrix1), IllegalArgumentException.class);
	}

	@Test
	public void denseMatrixCanBeMultipliedElementWiseWithSameSizeMatrix() {
		// Arrange:
		final Matrix matrix1 = new DenseMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });
		final Matrix matrix2 = new DenseMatrix(3, 2, new double[] { 7, 3, 1, 5, 11, 9 });

		// Act:
		final Matrix result = matrix1.multiplyElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 14, 9, 5, 55, 11, 72 })));
	}

	@Test
	public void sparseMatrixCanBeMultipliedElementWiseWithSameSizeMatrix() {
		// Arrange:
		final DenseMatrix matrix1 = new DenseMatrix(3, 2);
		matrix1.setAll(1);
		matrix1.setAll(new double[] { 1, 1, 2, 3, 1, 5 });

		final DenseMatrix matrix2 = new DenseMatrix(3, 2);
		matrix2.setAll(2);
		matrix2.setAt(1, 1, 5);

		// Act:
		final Matrix result = matrix1.multiplyElementWise(matrix2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 2, 2, 4, 15, 2, 10 })));
	}

	//endregion

	//region scale

	// TODO:
//	@Test
//	public void denseMatrixCanBeScaled() {
//		// Arrange:
//		final Matrix matrix = new DenseMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });
//
//		// Act:
//		matrix.scale(10);
//
//		// Assert:
//		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 0.2, 0.3, 0.5, 1.1, 0.1, 0.8 })));
//	}

	// TODO:
//	@Test
//	public void sparseMatrixCanBeScaled() {
//		// Arrange:
//		final DenseMatrix matrix = new DenseMatrix(3, 2);
//		matrix.setAll(1);
//		matrix.setAt(1, 1, 5);
//
//		// Act:
//		matrix.scale(5);
//
//		// Assert:
//		Assert.assertThat(matrix, IsEqual.equalTo(new DenseMatrix(3, 2, new double[] { 0.2, 0.2, 0.2, 1.0, 0.2, 0.2 })));
//	}

	//endregion

	//region isSameSize / isSparse

	@Test
	public void isSameSizeReturnsTrueWhenMatriciesHaveSameSize() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(3, 2);

		// Assert:
		Assert.assertThat(matrix.isSameSize(new DenseMatrix(3, 2)), IsEqual.equalTo(true));
		Assert.assertThat(matrix.isSameSize(new DenseMatrix(2, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new DenseMatrix(4, 2)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new DenseMatrix(3, 1)), IsEqual.equalTo(false));
		Assert.assertThat(matrix.isSameSize(new DenseMatrix(3, 3)), IsEqual.equalTo(false));
	}

	// TODO: review
//	@Test
//	public void isSameSizeReturnsTrueWhenMatrixIsSparse() {
//		// Arrange:
//		final Matrix sparseMatrix = new DenseMatrix(2, 3);
//		sparseMatrix.setAll(7.0);
//		sparseMatrix.setAt(1, 0, 8);
//
//		final Matrix denseMatrix = new DenseMatrix(2, 3, new double[]{ 7, 7, 7, 8, 7, 7 });
//
//		// Assert:
//		Assert.assertThat(sparseMatrix.isSparse(), IsEqual.equalTo(true));
//		Assert.assertThat(denseMatrix.isSparse(), IsEqual.equalTo(false));
//	}

	//endregion

	//region toString

	@Test
	public void denseMatrixStringRepresentationIsCorrect() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(3, 2, new double[] {
				2.1234, 11.1234, 3.2345, 1, 5012.0126, 8
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

	//region equals / hashCode

	@Test
	public void equalsReturnsFalseForNonMatrixObjects() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(matrix)));
		Assert.assertThat(new double[] { 0, 0, 7, 0, 0, 5 }, IsNot.not(IsEqual.equalTo((Object)matrix)));
	}

	@Test
	public void equalsReturnsFalseForMatrixObjectsWithDifferentDimensions() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });
		final Matrix matrix2 = new DenseMatrix(3, 2, new double[]{ 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(matrix2, IsNot.not(IsEqual.equalTo(matrix)));
		Assert.assertThat(new DenseMatrix(3, 3), IsNot.not(IsEqual.equalTo(matrix)));
		Assert.assertThat(new DenseMatrix(2, 4), IsNot.not(IsEqual.equalTo(matrix)));
	}

	@Test
	public void equalsReturnsFalseForSparseMatricesWithDifferentDefaultValues() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(2, 3);
		final DenseMatrix matrix2 = new DenseMatrix(2, 3);

		// Act:
		matrix.setAll(7);
		matrix2.setAll(8);

		// Assert:
		Assert.assertThat(matrix2, IsNot.not(IsEqual.equalTo(matrix)));
	}

	@Test
	public void equalsReturnsTrueForEquivalentDenseMatricesWithDifferentDefaultValues() {
		// Arrange:
		final DenseMatrix matrix = new DenseMatrix(1, 2);
		final DenseMatrix matrix2 = new DenseMatrix(1, 2);

		// Act:
		matrix.setAll(7);
		matrix.setAll(new double[] { 4, 9 });

		matrix2.setAll(8);
		matrix2.setAll(new double[] { 4, 9 });

		// Assert:
		Assert.assertThat(matrix2, IsEqual.equalTo(matrix));
	}

	@Test
	public void equalsReturnsTrueForEquivalentSparseMatrices() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(100, 1000);
		final Matrix matrix2 = new DenseMatrix(100, 1000);

		// Act:
		matrix.setAt(50, 50, 4);
		matrix.setAt(99, 200, 9);

		matrix2.setAt(50, 50, 4);
		matrix2.setAt(99, 200, 9);

		// Assert:
		Assert.assertThat(matrix2, IsEqual.equalTo(matrix));
	}

	@Test
	public void equalsReturnsTrueForEquivalentDenseMatrices() {
		// Arrange:
		final Matrix matrix = new DenseMatrix(2, 3, new double[]{ 0, 0, 7, 0, 0, 5 });
		final Matrix matrix2 = new DenseMatrix(2, 3, new double[]{ 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(matrix2, IsEqual.equalTo(matrix));
	}

	@Test
	public void equalsReturnsTrueForEquivalentDenseAndSparseMatrices() {
		// Arrange:
		final DenseMatrix sparseMatrix = new DenseMatrix(2, 3);
		sparseMatrix.setAll(7.0);
		sparseMatrix.setAt(1, 0, 8);

		final Matrix denseMatrix = new DenseMatrix(2, 3, new double[]{ 7, 7, 7, 8, 7, 7 });

		// Assert:
		Assert.assertThat(sparseMatrix, IsEqual.equalTo(denseMatrix));
		Assert.assertThat(denseMatrix, IsEqual.equalTo(sparseMatrix));
	}

	@Test
	public void hashCodesAreEqualForObjectsWithEquivalentDimensions() {
		// Arrange:
		final Matrix matrix1 = new DenseMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(new DenseMatrix(2, 3).hashCode(), IsEqual.equalTo(matrix1.hashCode()));
		Assert.assertThat(new DenseMatrix(3, 2).hashCode(), IsEqual.equalTo(matrix1.hashCode()));
		Assert.assertThat(new DenseMatrix(2, 2).hashCode(), IsNot.not(IsEqual.equalTo(matrix1.hashCode())));
		Assert.assertThat(new DenseMatrix(2, 4).hashCode(), IsNot.not(IsEqual.equalTo(matrix1.hashCode())));
	}

	//endregion
}
