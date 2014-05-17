package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DenseMatrixTest extends MatrixTest {

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

	//region setAll

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

	@Override
	protected Matrix createMatrix(int rows, int cols) {
		return new DenseMatrix(rows, cols);
	}

	@Override
	protected Matrix createMatrix(int rows, int cols, double[] values) {
		return new DenseMatrix(rows, cols, values);
	}
}
