package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class VectorTest {

	//region constructor / getAt / setAt

	@Test
	public void vectorIsInitializedToZero() {
		// Arrange:
		final Vector vector = new Vector(3);

		// Assert:
		Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void vectorValuesCanBeSet() {
		// Arrange:
		final Vector vector = new Vector(3);

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
			final Vector vector = new Vector(size);

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
		final Vector vector = new Vector(3);

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
		final Vector vector = new Vector(3);
		vector.setAt(0, 7);
		vector.setAt(1, -3);
		vector.setAt(2, 5);

		// Assert:
		Assert.assertThat(vector.sum(), IsEqual.equalTo(9.0));
	}

	@Test
	public void vectorAbsSumCanBeCalculated() {
		// Arrange:
		final Vector vector = new Vector(3);
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
		final Vector vector = new Vector(3);
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
		final Vector vector = new Vector(3);
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

	//region normalize

	@Test
	public void vectorWithNonZeroSumCanBeNormalized() {
		// Arrange:
		final Vector vector = new Vector(3);
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
		final Vector vector = new Vector(3);
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
		final Vector vector = new Vector(3);

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
		final Vector a = new Vector(3);
		a.setAt(0, 7);
		a.setAt(1, 5);
		a.setAt(2, 11);

		final Vector b = new Vector(3);
		b.setAt(0, 2);
		b.setAt(1, -4);
		b.setAt(2, 1);

		// Act:
		final Vector result = a.add(b);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(b)));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(9.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(1.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(12.0));
	}

	@Test(expected = InvalidParameterException.class)
	public void smallerVectorCannotBeAddedToLargerVector() {
		// Arrange:
		final Vector largerVector = new Vector(8);
		final Vector smallerVector = new Vector(7);

		// Act:
		largerVector.add(smallerVector);
	}

	@Test(expected = InvalidParameterException.class)
	public void largerVectorCannotBeAddedToSmallerVector() {
		// Arrange:
		final Vector largerVector = new Vector(8);
		final Vector smallerVector = new Vector(7);

		// Act:
		smallerVector.add(largerVector);
	}

	//endregion

	//region add

	@Test
	public void distanceCanBeCalculatedBetweenTwoVectorsOfSameSize() {
		// Arrange:
		final Vector a = new Vector(3);
		a.setAt(0, 7);
		a.setAt(1, 5);
		a.setAt(2, 11);

		final Vector b = new Vector(3);
		b.setAt(0, 2);
		b.setAt(1, -4);
		b.setAt(2, 1);

		// Act:
		final double distance = a.distance(b);

		// Assert:
		Assert.assertEquals(14.3527, distance, 0.0000001);
	}

	@Test(expected = InvalidParameterException.class)
	public void distanceCannotBeCalculatedFromSmallerVectorToLargerVector() {
		// Arrange:
		final Vector largerVector = new Vector(8);
		final Vector smallerVector = new Vector(7);

		// Act:
		largerVector.distance(smallerVector);
	}

	@Test(expected = InvalidParameterException.class)
	public void distanceCannotBeCalculatedFromLargerVectorToSmallerVector() {
		// Arrange:
		final Vector largerVector = new Vector(8);
		final Vector smallerVector = new Vector(7);

		// Act:
		smallerVector.distance(largerVector);
	}

	//endregion

	//region multiply

	@Test
	public void vectorCanBeMultipliedByScalar() {
		// Arrange:
		final Vector a = new Vector(3);
		a.setAt(0, 2);
		a.setAt(1, -4);
		a.setAt(2, 1);

		// Act:
		final Vector result = a.multiply(8);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(a)));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(16.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(-32.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(8.0));
	}

	@Test
	public void vectorCanBeMultipliedByMatrix() {
		// Arrange:
		final Vector v = new Vector(2);
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
		final Vector result = v.multiply(matrix);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v)));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(28.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(11.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(31.0));
	}

	@Test(expected = InvalidParameterException.class)
	public void vectorCannotBeMultipliedByMatrixWithFewerColumns() {
		// Arrange:
		final Vector v = new Vector(2);
		final Matrix m = new Matrix(2, 1);

		// Act:
		v.multiply(m);
	}

	@Test(expected = InvalidParameterException.class)
	public void vectorCannotBeMultipliedByMatrixWithMoreColumns() {
		// Arrange:
		final Vector v = new Vector(2);
		final Matrix m = new Matrix(2, 3);

		// Act:
		v.multiply(m);
	}

	//endregion

	//region multiplyElementWise

	@Test
	public void vectorCanBeMultipliedByVectorElementWise() {
		// Arrange:
		final Vector v1 = new Vector(3);
		v1.setAt(0, 3);
		v1.setAt(1, 7);
		v1.setAt(2, 2);

		final Vector v2 = new Vector(3);
		v2.setAt(0, 1);
		v2.setAt(1, 5);
		v2.setAt(2, 3);

		// Act:
		final Vector result = v1.multiplyElementWise(v2);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v1)));
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(v2)));
		Assert.assertThat(result.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(result.getAt(0), IsEqual.equalTo(3.0));
		Assert.assertThat(result.getAt(1), IsEqual.equalTo(35.0));
		Assert.assertThat(result.getAt(2), IsEqual.equalTo(6.0));
	}

	@Test(expected = InvalidParameterException.class)
	public void vectorCannotBeMultipliedByVectorWithFewerColumns() {
		// Arrange:
		final Vector v1 = new Vector(2);
		final Vector v2 = new Vector(3);

		// Act:
		v1.multiplyElementWise(v2);
	}

	@Test(expected = InvalidParameterException.class)
	public void vectorCannotBeMultipliedByVectorWithMoreColumns() {
		// Arrange:
		final Vector v1 = new Vector(3);
		final Vector v2 = new Vector(2);

		// Act:
		v1.multiplyElementWise(v2);
	}

	//endregion

	//region toString

	@Test
	public void vectorStringRepresentationIsCorrect() {
		// Arrange:
		final Vector vector = new Vector(6);
		vector.setAt(0, 2.1234);
		vector.setAt(1, 3.2345);
		vector.setAt(2, 5.0126);
		vector.setAt(3, 11.1234);
		vector.setAt(4, 1);
		vector.setAt(5, 8);

		// Assert:
		final String expectedResult = "2.123 3.234 5.013 11.123 1.000 8.000";

		// Assert:
		Assert.assertThat(vector.toString(), IsEqual.equalTo(expectedResult));

	}

	//endregion
}
