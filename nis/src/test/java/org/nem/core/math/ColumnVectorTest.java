package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class ColumnVectorTest {

	//region constructor / getAt / setAt

	@Test
	public void vectorIsInitializedToZero() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Assert:
		Assert.assertThat(vector.size(), IsEqual.equalTo(3));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void vectorCanBeInitializedAroundRawVector() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(9.0, 3.2, 5.4);

		// Assert:
		Assert.assertThat(vector.size(), IsEqual.equalTo(3));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(9.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(3.2));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(5.4));
	}

	@Test
	public void vectorMustHaveNonZeroSize() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new ColumnVector(0), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new ColumnVector(null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new ColumnVector(), IllegalArgumentException.class);
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
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(7, 3, 5)));
	}

	@Test
	public void vectorValuesCanBeIncremented() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Act:
		vector.setAt(0, 7);
		vector.setAt(1, 3);
		vector.setAt(2, 5);
		vector.incrementAt(0, 6);
		vector.incrementAt(1, 4);
		vector.incrementAt(2, 1);

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(13, 7, 6)));
	}

	@Test
	public void vectorCannotBeIndexedOutOfBounds() {
		// Assert:
		assertOutOfBounds(3, -1);
		assertOutOfBounds(3, 3);
	}

	private static void assertOutOfBounds(final int size, final int index) {
		ExceptionAssert.assertThrows(v -> {
			// Arrange:
			final ColumnVector vector = new ColumnVector(size);

			// Act:
			vector.getAt(index);
		}, ArrayIndexOutOfBoundsException.class);
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
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(4, 4, 4)));
	}

	//endregion

	//region sum / absSum

	@Test
	public void vectorSumCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(7, -3, 5);

		// Assert:
		Assert.assertThat(vector.sum(), IsEqual.equalTo(9.0));
	}

	@Test
	public void vectorAbsSumCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(7, -3, 5);

		// Assert:
		Assert.assertThat(vector.absSum(), IsEqual.equalTo(15.0));
	}

	//endregion

	//region max / median

	@Test
	public void vectorMaxCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(7, 11, 5);

		// Assert:
		Assert.assertThat(vector.max(), IsEqual.equalTo(11.0));
	}

	@Test
	public void vectorMedianCanBeCalculated() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(7, 11, 5);

		// Assert:
		Assert.assertThat(vector.median(), IsEqual.equalTo(7.0));
	}

	//endregion

	//region align

	@Test
	public void cannotAlignVectorWithNonZeroInFirstPosition() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(0, -6, 14);

		// Act:
		boolean result = vector.align();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0, -6, 14)));
	}

	@Test
	public void canAlignVectorWithNonZeroValueInFirstPosition() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(-4, -6, 14);

		// Act:
		boolean result = vector.align();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(1, 1.5, -3.5)));
	}

	//endregion

	//region scale

	@Test
	public void vectorCanBeScaledByArbitraryFactor() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(2, -4, 1);

		// Act:
		vector.scale(8);

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.25, -0.50, 0.125)));
	}

	//endregion

	//region normalize

	@Test
	public void vectorWithNonZeroSumCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3, 5, 2);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.3, 0.5, 0.2)));
	}

	@Test
	public void vectorWithNegativeValuesCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3, -5, 2);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.3, -0.5, 0.2)));
	}

	@Test
	public void zeroVectorCanBeNormalized() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(3);

		// Act:
		vector.normalize();

		// Assert:
		Assert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0, 0, 0)));
	}

	//endregion

	//region add

	@Test
	public void twoVectorsOfSameSizeCanBeAddedTogether() {
		// Arrange:
		final ColumnVector a = new ColumnVector(7, 5, 11);
		final ColumnVector b = new ColumnVector(2, -4, 1);

		// Act:
		final ColumnVector result = a.add(b);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(b)));
		Assert.assertThat(result, IsEqual.equalTo(new ColumnVector(9, 1, 12)));
	}

	@Test
	public void differentSizedVectorsCannotBeAddedTogether() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		ExceptionAssert.assertThrows(v -> largerVector.add(smallerVector), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> smallerVector.add(largerVector), IllegalArgumentException.class);
	}

	//endregion

	//region distance / getMagnitude

	@Test
	public void l1DistanceCanBeCalculatedBetweenTwoVectorsOfSameSize() {
		// Arrange:
		final ColumnVector a = new ColumnVector(7, 5, 11);
		final ColumnVector b = new ColumnVector(2, -4, 1);

		// Act:
		final double distance = a.l1Distance(b);

		// Assert:
		Assert.assertEquals(24.0, distance, 0.0000001);
	}

	@Test
	public void l1DistanceCannotBeCalculatedForDifferentSizedVectors() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		ExceptionAssert.assertThrows(v -> largerVector.l1Distance(smallerVector), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> smallerVector.l1Distance(largerVector), IllegalArgumentException.class);
	}

	@Test
	public void l2DistanceCanBeCalculatedBetweenTwoVectorsOfSameSize() {
		// Arrange:
		final ColumnVector a = new ColumnVector(7, 5, 11);
		final ColumnVector b = new ColumnVector(2, -4, 1);

		// Act:
		final double distance = a.l2Distance(b);

		// Assert:
		Assert.assertEquals(14.3527, distance, 0.0000001);
	}

	@Test
	public void l2DistanceCannotBeCalculatedForDifferentSizedVectors() {
		// Arrange:
		final ColumnVector largerVector = new ColumnVector(8);
		final ColumnVector smallerVector = new ColumnVector(7);

		// Act:
		ExceptionAssert.assertThrows(v -> largerVector.l2Distance(smallerVector), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> smallerVector.l2Distance(largerVector), IllegalArgumentException.class);
	}

	@Test
	public void magnitudeCanBeCalculatedForVector() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(7, 5, 11);

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
		final ColumnVector a = new ColumnVector(2, -4, 1);

		// Act:
		final ColumnVector result = a.multiply(8);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result, IsEqual.equalTo(new ColumnVector(16, -32, 8)));
	}

	//endregion

	//region multiplyElementWise

	@Test
	public void vectorCanBeMultipliedByVectorElementWise() {
		// Arrange:
		final ColumnVector v1 = new ColumnVector(3, 7, 2);
		final ColumnVector v2 = new ColumnVector(1, 5, 3);

		// Act:
		final ColumnVector result = v1.multiplyElementWise(v2);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v1)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v2)));
		Assert.assertThat(result, IsEqual.equalTo(new ColumnVector(3, 35, 6)));
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

	//region roundTo

	@Test
	public void vectorCanBeRounded() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(0.00024452, -0.123, 0.577);

		// Act:
		final ColumnVector result = vector.roundTo(2);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(result, IsEqual.equalTo(new ColumnVector(0.00, -0.12, 0.58)));
	}

	//endregion

	//region roundTo

	@Test
	public void vectorCanBeSquareRooted() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(625, 36, 121);

		// Act:
		final ColumnVector result = vector.sqrt();

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(result, IsEqual.equalTo(new ColumnVector(25.0, 6.0, 11.0)));
	}

	//endregion

	//region toString

	@Test
	public void vectorStringRepresentationIsCorrect() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(2.1234, 3.2345, 5012.0126, 11.1234, 1, 8);

		// Assert:
		final String expectedResult = "2.123 3.235 5012.013 11.123 1.000 8.000";

		// Assert:
		Assert.assertThat(vector.toString(), IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(2, -4, 1);

		// Assert:
		Assert.assertThat(new ColumnVector(2, -4, 1), IsEqual.equalTo(vector));
		Assert.assertThat(new ColumnVector(1, -4, 1), IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(new ColumnVector(2, 8, 1), IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(new ColumnVector(2, -4, 2), IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(vector)));
		Assert.assertThat(new double[] { 2, -4, 1 }, IsNot.not(IsEqual.equalTo((Object)vector)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final ColumnVector vector = new ColumnVector(2, -4, 1);
		int hashCode = vector.hashCode();

		// Assert:
		Assert.assertThat(new ColumnVector(2, -4, 1).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new ColumnVector(1, -4, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new ColumnVector(2, 8, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new ColumnVector(2, -4, 2).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion
}