package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public abstract class MatrixTest<TMatrix extends Matrix> {

	//region createMatrix

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	protected abstract TMatrix createMatrix(final int rows, final int cols);

	/**
	 * Creates a new matrix of the specified size and initial values.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 * @param values The initial values.
	 */
	protected TMatrix createMatrix(final int rows, final int cols, final double[] values) {
		final TMatrix matrix = this.createMatrix(rows, cols);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final double value = values[i * cols + j];
				if (0 == value) {
					continue;
				}

				matrix.setAt(i, j, value);
			}
		}

		return matrix;
	}

	//endregion

	//region constructor

	@Test
	public void matrixIsInitializedToZero() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3);

		// Assert:
		Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
		Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		Assert.assertThat(matrix.getElementCount(), IsEqual.equalTo(6));
		Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(0.0));
	}

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
		this.assertGetOutOfBounds(2, 3, -1, 0);
		this.assertGetOutOfBounds(2, 3, 0, -1);
		this.assertGetOutOfBounds(2, 3, 2, 0);
		this.assertGetOutOfBounds(2, 3, 0, 3);
	}

	@Test
	public void matrixSetCannotBeIndexedOutOfBounds() {
		// Assert:
		this.assertSetOutOfBounds(2, 3, -1, 0);
		this.assertSetOutOfBounds(2, 3, 0, -1);
		this.assertSetOutOfBounds(2, 3, 2, 0);
		this.assertSetOutOfBounds(2, 3, 0, 3);
	}

	private void assertGetOutOfBounds(final int numRows, final int numCols, final int row, final int col) {
		// Assert:
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final Matrix matrix = this.createMatrix(numRows, numCols);

			// Act:
			matrix.getAt(row, col);
		}, IndexOutOfBoundsException.class);
	}

	private void assertSetOutOfBounds(final int numRows, final int numCols, final int row, final int col) {
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
		final Collection<Integer> zeroColumnIndexes = matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.2, 0.55, 0.3, 0.05, 0.5, 0.4 })));
		Assert.assertThat(zeroColumnIndexes, IsEqual.equalTo(Collections.emptyList()));
	}

	@Test
	public void zeroMatrixCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3);

		// Act:
		final Collection<Integer> zeroColumnIndexes = matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 0, 0, 0, 0, 0, 0 })));
		Assert.assertThat(zeroColumnIndexes, IsEqual.equalTo(Arrays.asList(0, 1, 2)));
	}

	@Test
	public void matrixWithZeroSumColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 2, 0, 0, 0, -2, 0 });

		// Act:
		final Collection<Integer> zeroColumnIndexes = matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(matrix, IsEqual.equalTo(this.createMatrix(2, 3, new double[] { 1, 0, 0, 0, -1, 0 })));
		Assert.assertThat(zeroColumnIndexes, IsEqual.equalTo(Collections.singletonList(2)));
	}

	@Test
	public void allSparseMatrixColumnsCanBeNormalized() {
		// Arrange:
		final Matrix matrix = this.createMatrix(4, 2);
		matrix.setAt(0, 1, 4);
		matrix.setAt(2, 1, 12);

		// Act:
		final Collection<Integer> zeroColumnIndexes = matrix.normalizeColumns();

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(this.createMatrix(4, 2, new double[] { 0, 0.25, 0, 0, 0, 0.75, 0, 0 })));
		Assert.assertThat(zeroColumnIndexes, IsEqual.equalTo(Collections.singletonList(0)));
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

	//region roundTo

	@Test
	public void denseMatrixCanBeRounded() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2.1234, 11.1234, 3.2345, 1, 5012.0126, 8 });

		// Act:
		final Matrix roundedMatrix = matrix.roundTo(2);

		// Assert:
		Assert.assertThat(
				roundedMatrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 2.12, 11.12, 3.23, 1, 5012.01, 8 })));
	}

	@Test
	public void sparseMatrixCanBeRounded() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(2, 0, 5012.0126);
		matrix.setAt(1, 1, 11.1234);

		// Act:
		final Matrix roundedMatrix = matrix.roundTo(1);

		// Assert:
		Assert.assertThat(
				roundedMatrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0, 0, 0, 11.1, 5012.0, 0 })));
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

	//region add

	@Test
	public void matrixCanBeAddedToByScalar() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });

		// Act:
		final Matrix result = matrix.add(0.1);

		// Assert:
		Assert.assertThat(
				result.roundTo(5),
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 2.1, 3.1, 5.1, 11.1, 1.1, 8.1 })));
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

	//region multiply

	@Test
	public void denseMatrixCanBeMultipliedByScalar() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 3, 5, 11, 1, 8 });

		// Act:
		final Matrix result = matrix.multiply(0.1);

		// Assert:
		Assert.assertThat(
				result.roundTo(5),
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.2, 0.3, 0.5, 1.1, 0.1, 0.8 })));
	}

	@Test
	public void sparseMatrixCanBeMultipliedByScalar() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2);
		matrix.setAt(2, 0, 3);
		matrix.setAt(1, 1, 5);

		// Act::
		final Matrix result = matrix.multiply(0.2);

		// Assert:
		Assert.assertThat(
				result.roundTo(5),
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0.0, 0.0, 0.0, 1.0, 0.6, 0.0 })));
	}

	@Test
	public void matrixCannotBeMultipliedByVectorOfDifferentSize() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, -3, -5, 11, -1, 8 });

		// Act:
		ExceptionAssert.assertThrows(v -> matrix.multiply(new ColumnVector(1)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> matrix.multiply(new ColumnVector(3)), IllegalArgumentException.class);
	}

	@Test
	public void matrixCanBeMultipliedByVectorOfSameSize() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 11, -3, -1, -5, 8 });
		final ColumnVector vector = new ColumnVector(2, 3);

		// Act:
		final ColumnVector result = matrix.multiply(vector);

		// Assert:
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(37.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-9.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(14.0));
	}

	@Test
	public void sparseMatrixCanBeMultipliedByVectorOfSameSize() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, 0, 0, -1, -5, 0 });
		final ColumnVector vector = new ColumnVector(2, 3);

		// Act:
		final ColumnVector result = matrix.multiply(vector);

		// Assert:
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(4.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-3.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(-10.0));
	}

	//endregion

	//region abs / sqrt

	@Test
	public void absoluteValueOfMatrixCanBeTaken() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 2, -3, -5, 0, -1, 8 });

		// Act:
		final Matrix result = matrix.abs();

		// Assert:
		Assert.assertThat(
				result,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 2, 3, 5, 0, 1, 8 })));
	}

	@Test
	public void squareRootOfMatrixCanBeTaken() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { 625, 4, 0, 36, 49, 121 });

		// Act:
		final Matrix result = matrix.sqrt();

		// Assert:
		Assert.assertThat(
				result,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 25, 2, 0, 6, 7, 11 })));
	}

	//endregion

	//region removeNegatives / removeLessThan

	@Test
	public void removeNegativesZerosOutAllElementsWithNegativeValues() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { -3, 2, -5, 0, -1, 8 });

		// Act:
		matrix.removeNegatives();

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0, 2, 0, 0, 0, 8 })));
	}

	@Test
	public void removeLessThanZerosOutAllElementsLessThanSpecifiedValue() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] { -3, 1.4, 1.6, 1.5, -1, 1 });

		// Act:
		matrix.removeLessThan(1.5);

		// Assert:
		Assert.assertThat(
				matrix,
				IsEqual.equalTo(this.createMatrix(3, 2, new double[] { 0, 0, 1.6, 1.5, 0, 0 })));
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

	//region isZeroMatrix

	@Test
	public void isZeroMatrixReturnsTrueIfAndOnlyIfAllElementsAreZero() {
		// Assert:
		Assert.assertThat(
				this.createMatrix(3, 2, new double[] { -3, 2, -5, 7, -1, 8 }).isZeroMatrix(),
				IsEqual.equalTo(false));
		Assert.assertThat(
				this.createMatrix(3, 2, new double[] { -3, 2, -5, 0, -1, 8 }).isZeroMatrix(),
				IsEqual.equalTo(false));
		Assert.assertThat(
				this.createMatrix(3, 2, new double[] { 0, 0, -1, 1, 0, 0 }).isZeroMatrix(),
				IsEqual.equalTo(false));
		Assert.assertThat(
				this.createMatrix(3, 2, new double[] { 0, 0, -1, 0, 0, 0 }).isZeroMatrix(),
				IsEqual.equalTo(false));
		Assert.assertThat(
				this.createMatrix(3, 2, new double[] { 0, 0, 0, 0, 0, 0 }).isZeroMatrix(),
				IsEqual.equalTo(true));
	}

	//endregion

	//region getNonZeroElementRowIterator

	@Test
	public void hasNextReturnsTrueIfMoreColumnsAreAvailable() {
		// Arrange:
		final TMatrix matrix = this.createMatrixForIteratorTests();
		final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(0);

		// Act + Assert
		for (int i = 0; i < 3; i++) {
			Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
			iterator.next();
		}
	}

	@Test
	public void hasNextReturnsFalseImmediatelyForZeroRow() {
		// Arrange:
		final TMatrix matrix = this.createMatrixForIteratorTests();
		final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(1);

		// Assert:
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
		ExceptionAssert.assertThrows(v -> iterator.next(), IndexOutOfBoundsException.class);
	}

	@Test
	public void hasNextReturnsFalseAfterAllElementsHaveBeenIteratedForNonZeroRow() {
		// Arrange:
		final TMatrix matrix = this.createMatrixForIteratorTests();
		final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(0);

		// Act:
		for (int i = 0; i < 3; i++) {
			iterator.next();
		}

		// Assert:
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
		ExceptionAssert.assertThrows(v -> iterator.next(), IndexOutOfBoundsException.class);
	}

	@Test
	public void nextReturnsCorrectMatrixElementsInOrder() {
		// Arrange:
		final TMatrix matrix = this.createMatrixForIteratorTests();
		final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(0);

		// Act + Assert:
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 0, 1.1)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 1, 2.2)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new MatrixElement(0, 3, 4.4)));
	}

	private TMatrix createMatrixForIteratorTests() {
		final TMatrix matrix = this.createMatrix(4, 4);
		matrix.setAtUnchecked(0, 0, 1.1);
		matrix.setAtUnchecked(0, 3, 4.4);
		matrix.setAtUnchecked(0, 1, 2.2);
		return matrix;
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsReturnsFalseForNonMatrixObjects() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(matrix)));
		Assert.assertThat(new double[] { 0, 0, 7, 0, 0, 5 }, IsNot.not(IsEqual.equalTo((Object)matrix)));
	}

	@Test
	public void equalsReturnsFalseForMatrixObjectsWithDifferentDimensions() {
		// Arrange:
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });
		final Matrix matrix2 = this.createMatrix(3, 2, new double[] { 0, 0, 7, 0, 0, 5 });

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
		final Matrix matrix = this.createMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });
		final Matrix matrix2 = this.createMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(matrix2, IsEqual.equalTo(matrix));
	}

	@Test
	public void hashCodesAreEqualForObjectsWithEquivalentDimensions() {
		// Arrange:
		final Matrix matrix1 = this.createMatrix(2, 3, new double[] { 0, 0, 7, 0, 0, 5 });

		// Assert:
		Assert.assertThat(this.createMatrix(2, 3).hashCode(), IsEqual.equalTo(matrix1.hashCode()));
		Assert.assertThat(this.createMatrix(3, 2).hashCode(), IsEqual.equalTo(matrix1.hashCode()));
		Assert.assertThat(this.createMatrix(2, 2).hashCode(), IsNot.not(IsEqual.equalTo(matrix1.hashCode())));
		Assert.assertThat(this.createMatrix(2, 4).hashCode(), IsNot.not(IsEqual.equalTo(matrix1.hashCode())));
	}

	//endregion
}
