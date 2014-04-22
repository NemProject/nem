package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;

public class ColumnVectorTest {

	//region constructor / getAt / setAt

	@Test
	public void vectorIsInitializedToZero() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Assert:
		Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void vectorValuesCanBeSet() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Act:
		vector.setAt(0, 7);
		vector.setAt(1, 3);
		vector.setAt(2, 5);

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(7.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(3.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(5.0));
	}

	@Test
	public void vectorCannotBeIndexedOutOfBounds() {
		// Assert:
		assertOutOfBounds(3, -1);
		assertOutOfBounds(3, 3);
	}

	private static void assertOutOfBounds(final int size, final int index) {
		try {
			// Arrange:
			final ColumnVector vector = new ColumnVector(size);

			// Act:
			vector.getAt(index);

			// Assert:
			Assert.fail("expected exception was not thrown");
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
	}

	//endregion

	//region setAll

	@Test
	public void setAllSetsAllVectorElementValues() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Act:
		vector.setAll(4);

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(4.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(4.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(4.0));
	}

	//endregion

	//region sum / absSum

	@Test
	public void vectorSumCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 7);
		vector.setAt(1, -3);
		vector.setAt(2, 5);

		// Assert:
		Assert.assertThat(vector.sum(), IsEqual.equalTo(9.0));
	}

	@Test
	public void vectorAbsSumCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 7);
		vector.setAt(1, -3);
		vector.setAt(2, 5);

		// Assert:
		Assert.assertThat(vector.absSum(), IsEqual.equalTo(15.0));
	}

	//endregion

	//region align

	@Test
	public void cannotAlignVectorWithNonZeroInFirstPosition() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 0);
		vector.setAt(1, -6);
		vector.setAt(2, 14);

		// Act:
		boolean result = vector.align();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(-6.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(14.0));
	}

	@Test
	public void canAlignVectorWithNonZeroValueInFirstPosition() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, -4);
		vector.setAt(1, -6);
		vector.setAt(2, 14);

		// Act:
		boolean result = vector.align();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(1.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(1.5));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(-3.5));
	}

	//endregion

	//region scale

	@Test
	public void vectorCanBeScaledByArbitraryFactor() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 2);
		vector.setAt(1, -4);
		vector.setAt(2, 1);

		// Act:
		vector.scale(8);

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.25));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(-0.50));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.125));
	}

	//endregion

	//region normalize

	@Test
	public void vectorWithNonZeroSumCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 3);
		vector.setAt(1, 5);
		vector.setAt(2, 2);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.3));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.5));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.2));
	}

	@Test
	public void vectorWithNegativeValuesCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 3);
		vector.setAt(1, -5);
		vector.setAt(2, 2);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.3));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(-0.5));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.2));
	}

	@Test
	public void zeroVectorCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	//endregion

	//region add

	@Test
	public void twoVectorsOfSameSizeCanBeAddedTogether() {
		// Arrange:
		final ColumnVector a = new ColumnVector(3);
		a.setAt(0, 7);
		a.setAt(1, 5);
		a.setAt(2, 11);

		final ColumnVector b = new ColumnVector(3);
		b.setAt(0, 2);
		b.setAt(1, -4);
		b.setAt(2, 1);

		// Act:
		final ColumnVector result = a.add(b);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(b)));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(9.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(1.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(12.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void smallerVectorCannotBeAddedToLargerVector() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		largerVector.add(smallerVector);
	}

	@Test(expected = IllegalArgumentException.class)
	public void largerVectorCannotBeAddedToSmallerVector() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		smallerVector.add(largerVector);
	}

	//endregion

	//region distance / getMagnitude

	@Test
	public void distanceCanBeCalculatedBetweenTwoVectorsOfSameSize() {
		// Arrange:
		final ColumnVector a = new ColumnVector(3);
		a.setAt(0, 7);
		a.setAt(1, 5);
		a.setAt(2, 11);

		final ColumnVector b = new ColumnVector(3);
		b.setAt(0, 2);
		b.setAt(1, -4);
		b.setAt(2, 1);

		// Act:
		final double distance = a.distance(b);

		// Assert:
		Assert.assertEquals(14.3527, distance, 0.0000001);
	}

	@Test(expected = IllegalArgumentException.class)
	public void distanceCannotBeCalculatedFromSmallerVectorToLargerVector() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		largerVector.distance(smallerVector);
	}

	@Test(expected = IllegalArgumentException.class)
	public void distanceCannotBeCalculatedFromLargerVectorToSmallerVector() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		smallerVector.distance(largerVector);
	}

	@Test
	public void magnitudeCanBeCalculatedForVector() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);
		vector.setAt(0, 7);
		vector.setAt(1, 5);
		vector.setAt(2, 11);

		// Act:
		final double magnitude = vector.getMagnitude();

		// Assert:
		Assert.assertEquals(13.96424, magnitude, 0.0000001);
	}

	//endregion

	//region multiply

	@Test
	public void vectorCanBeMultipliedByScalar() {
		// Arrange:
		final ColumnVector a = new ColumnVector(3);
		a.setAt(0, 2);
		a.setAt(1, -4);
		a.setAt(2, 1);

		// Act:
		final ColumnVector result = a.multiply(8);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(16.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-32.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(8.0));
	}

	@Test
	public void vectorCanBeMultipliedByMatrix() {
		// Arrange:
		final ColumnVector v = new ColumnVector(2);
		v.setAt(0, 3);
		v.setAt(1, 2);

		final Matrix matrix = new Matrix(3, 2);
		matrix.setAt(0, 0, 2);
		matrix.setAt(1, 0, 3);
		matrix.setAt(2, 0, 5);
		matrix.setAt(0, 1, 11);
		matrix.setAt(1, 1, 1);
		matrix.setAt(2, 1, 8);

		// Act:
		final ColumnVector result = v.multiply(matrix);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v)));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(28.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(11.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(31.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void vectorCannotBeMultipliedByMatrixWithFewerColumns() {
		// Arrange:
		final ColumnVector v = new ColumnVector(2);
		final Matrix m = new Matrix(2, 1);

		// Act:
		v.multiply(m);
	}

	@Test(expected = IllegalArgumentException.class)
	public void vectorCannotBeMultipliedByMatrixWithMoreColumns() {
		// Arrange:
		final ColumnVector v = new ColumnVector(2);
		final Matrix m = new Matrix(2, 3);

		// Act:
		v.multiply(m);
	}

	//endregion

	//region multiplyElementWise

	@Test
	public void vectorCanBeMultipliedByVectorElementWise() {
		// Arrange:
		final ColumnVector v1 = new ColumnVector(3);
		v1.setAt(0, 3);
		v1.setAt(1, 7);
		v1.setAt(2, 2);

		final ColumnVector v2 = new ColumnVector(3);
		v2.setAt(0, 1);
		v2.setAt(1, 5);
		v2.setAt(2, 3);

		// Act:
		final ColumnVector result = v1.multiplyElementWise(v2);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v1)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v2)));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(3.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(35.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(6.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void vectorCannotBeMultipliedByVectorWithFewerColumns() {
		// Arrange:
		final ColumnVector v1 = new ColumnVector(2);
		final ColumnVector v2 = new ColumnVector(3);

		// Act:
		v1.multiplyElementWise(v2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void vectorCannotBeMultipliedByVectorWithMoreColumns() {
		// Arrange:
		final ColumnVector v1 = new ColumnVector(3);
		final ColumnVector v2 = new ColumnVector(2);

		// Act:
		v1.multiplyElementWise(v2);
	}

	//endregion

	//region toString

	@Test
	public void vectorStringRepresentationIsCorrect() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(6);
		vector.setAt(0, 2.1234);
		vector.setAt(1, 3.2345);
		vector.setAt(2, 5012.0126);
		vector.setAt(3, 11.1234);
		vector.setAt(4, 1);
		vector.setAt(5, 8);

		// Assert:
		final String expectedResult = "2.123 3.235 5012.013 11.123 1.000 8.000";

		// Assert:
		Assert.assertThat(vector.toString(), IsEqual.equalTo(expectedResult));

	}

	//endregion
}
