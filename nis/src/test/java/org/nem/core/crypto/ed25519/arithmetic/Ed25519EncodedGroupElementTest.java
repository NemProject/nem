package org.nem.core.crypto.ed25519.arithmetic;

import org.hamcrest.core.*;
import org.junit.*;

public class Ed25519EncodedGroupElementTest {

	@Test
	public void canBeCreatedFromByteArray() {
		// Assert:
		new Ed25519EncodedGroupElement(new byte[32]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedFromArrayWithIncorrectLength() {
		// Assert:
		new Ed25519EncodedGroupElement(new byte[30]);
	}

	// region getRaw

	@Test
	public void getRawReturnsUnderlyingArray() {
		// Act:
		final byte[] values = new byte[32];
		values[0] = 5;
		values[6] = 15;
		values[23] = -67;
		final Ed25519EncodedGroupElement encoded = new Ed25519EncodedGroupElement(values);

		// Assert:
		Assert.assertThat(values, IsEqual.equalTo(encoded.getRaw()));
	}

	// endregion

	// region encode / decode

	@Test
	public void decodePlusEncodeDoesNotAlterTheEncodedGroupElement() {
		// Act:
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedGroupElement original = MathUtils.getRandomEncodedGroupElement();
			final Ed25519EncodedGroupElement encoded = original.decode().encode();

			// Assert:
			Assert.assertThat(encoded, IsEqual.equalTo(original));
		}
	}

	// endregion

	@Test
	public void getAffineXReturnsExpectedResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedGroupElement encoded = MathUtils.getRandomGroupElement().encode();

			// Act:
			final Ed25519FieldElement affineX1 = encoded.getAffineX();
			final Ed25519FieldElement affineX2 = MathUtils.toRepresentation(encoded.decode(), CoordinateSystem.AFFINE).getX();

			// Assert:
			Assert.assertThat(affineX1, IsEqual.equalTo(affineX2));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAffineXThrowsIfEncodedGroupElementIsInvalid() {
		// Arrange:
		final Ed25519GroupElement g = Ed25519GroupElement.p2(Ed25519Field.ONE, Ed25519Field.D, Ed25519Field.ONE);
		final Ed25519EncodedGroupElement encoded = g.encode();

		// Assert:
		encoded.getAffineX();
	}

	@Test
	public void getAffineYReturnsExpectedResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedGroupElement encoded = MathUtils.getRandomEncodedGroupElement();

			// Act:
			final Ed25519FieldElement affineY1 = encoded.getAffineY();
			final Ed25519FieldElement affineY2 = MathUtils.toRepresentation(encoded.decode(), CoordinateSystem.AFFINE).getY();

			// Assert:
			Assert.assertThat(affineY1, IsEqual.equalTo(affineY2));
		}
	}

	// region hashCode / equals

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Ed25519EncodedGroupElement g1 = MathUtils.getRandomEncodedGroupElement();
		final Ed25519EncodedGroupElement g2 = g1.decode().encode();
		final Ed25519EncodedGroupElement g3 = MathUtils.getRandomEncodedGroupElement();
		final Ed25519EncodedGroupElement g4 = MathUtils.getRandomEncodedGroupElement();

		// Assert
		Assert.assertThat(g2, IsEqual.equalTo(g1));
		Assert.assertThat(g1, IsNot.not(IsEqual.equalTo(g3)));
		Assert.assertThat(g2, IsNot.not(IsEqual.equalTo(g4)));
		Assert.assertThat(g3, IsNot.not(IsEqual.equalTo(g4)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Ed25519EncodedGroupElement g1 = MathUtils.getRandomEncodedGroupElement();
		final Ed25519EncodedGroupElement g2 = g1.decode().encode();
		final Ed25519EncodedGroupElement g3 = MathUtils.getRandomEncodedGroupElement();
		final Ed25519EncodedGroupElement g4 = MathUtils.getRandomEncodedGroupElement();

		// Assert
		Assert.assertThat(g2.hashCode(), IsEqual.equalTo(g1.hashCode()));
		Assert.assertThat(g1.hashCode(), IsNot.not(IsEqual.equalTo(g3.hashCode())));
		Assert.assertThat(g2.hashCode(), IsNot.not(IsEqual.equalTo(g4.hashCode())));
		Assert.assertThat(g3.hashCode(), IsNot.not(IsEqual.equalTo(g4.hashCode())));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final Ed25519EncodedGroupElement encoded = Ed25519GroupElement.p2(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE).encode();

		// Act:
		final String encodedAsString = encoded.toString();
		final String expectedString = String.format("x=%s\ny=%s\n",
				"0000000000000000000000000000000000000000000000000000000000000000",
				"0100000000000000000000000000000000000000000000000000000000000000");

		// Assert:
		Assert.assertThat(encodedAsString, IsEqual.equalTo(expectedString));
	}

	// endregion
}
