package org.nem.core.crypto.ed25519.arithmetic;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Ed25519EncodedFieldElementTest {

	// region constructor

	@Test
	public void canBeCreatedFromByteArrayWithLengthThirtyTwo() {
		// Assert:
		new Ed25519EncodedFieldElement(new byte[32]);
	}

	@Test
	public void canBeCreatedFromByteArrayWithLengthSixtyFour() {
		// Assert:
		new Ed25519EncodedFieldElement(new byte[64]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedFromArrayWithIncorrectLength() {
		// Assert:
		new Ed25519EncodedFieldElement(new byte[50]);
	}

	// endregion

	// region isNonZero

	@Test
	public void isNonZeroReturnsFalseIfEncodedFieldElementIsZero() {
		// Act:
		final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(new byte[32]);

		// Assert:
		Assert.assertThat(encoded.isNonZero(), IsEqual.equalTo(false));
	}

	@Test
	public void isNonZeroReturnsTrueIfEncodedFieldElementIsNonZero() {
		// Act:
		final byte[] values = new byte[32];
		values[0] = 5;
		final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(values);

		// Assert:
		Assert.assertThat(encoded.isNonZero(), IsEqual.equalTo(true));
	}

	// endregion

	// region getRaw

	@Test
	public void getRawReturnsUnderlyingArray() {
		// Act:
		final byte[] values = new byte[32];
		values[0] = 5;
		values[6] = 15;
		values[23] = -67;
		final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(values);

		// Assert:
		Assert.assertThat(values, IsEqual.equalTo(encoded.getRaw()));
	}

	// endregion

	// region encode / decode

	@Test
	public void decodePlusEncodeDoesNotAlterTheEncodedFieldElement() {
		// Act:
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedFieldElement original = MathUtils.getRandomEncodedFieldElement(32);
			final Ed25519EncodedFieldElement encoded = original.decode().encode();

			// Assert:
			Assert.assertThat(encoded, IsEqual.equalTo(original));
		}
	}

	// endregion

	// region modulo group order arithmetic

	@Test
	public void modQReturnsExpectedResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(MathUtils.getRandomByteArray(64));

			// Act:
			final Ed25519EncodedFieldElement reduced1 = encoded.modQ();
			final Ed25519EncodedFieldElement reduced2 = MathUtils.reduceModGroupOrder(encoded);

			// Assert:
			Assert.assertThat(MathUtils.toBigInteger(reduced1).compareTo(Ed25519Field.P), IsEqual.equalTo(-1));
			Assert.assertThat(MathUtils.toBigInteger(reduced1).compareTo(new BigInteger("-1")), IsEqual.equalTo(1));
			Assert.assertThat(reduced1, IsEqual.equalTo(reduced2));
		}
	}

	@Test
	public void multiplyAndAddModQReturnsExpectedResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519EncodedFieldElement encoded1 = MathUtils.getRandomEncodedFieldElement(32);
			final Ed25519EncodedFieldElement encoded2 = MathUtils.getRandomEncodedFieldElement(32);
			final Ed25519EncodedFieldElement encoded3 = MathUtils.getRandomEncodedFieldElement(32);

			// Act:
			final Ed25519EncodedFieldElement result1 = encoded1.multiplyAndAddModQ(encoded2, encoded3);
			final Ed25519EncodedFieldElement result2 = MathUtils.multiplyAndAddModGroupOrder(encoded1, encoded2, encoded3);

			// Assert:
			Assert.assertThat(MathUtils.toBigInteger(result1).compareTo(Ed25519Field.P), IsEqual.equalTo(-1));
			Assert.assertThat(MathUtils.toBigInteger(result1).compareTo(new BigInteger("-1")), IsEqual.equalTo(1));
			Assert.assertThat(result1, IsEqual.equalTo(result2));
		}
	}

	// endregion

	// region encode

	@Test
	public void encodeReturnsCorrectByteArrayForSimpleFieldElements() {
		// Arrange:
		final int[] t1 = new int[10];
		final int[] t2 = new int[10];
		t2[0] = 1;
		final Ed25519FieldElement fieldElement1 = new Ed25519FieldElement(t1);
		final Ed25519FieldElement fieldElement2 = new Ed25519FieldElement(t2);

		// Act:
		final Ed25519EncodedFieldElement encoded1 = fieldElement1.encode();
		final Ed25519EncodedFieldElement encoded2 = fieldElement2.encode();

		// Assert:
		Assert.assertThat(encoded1, IsEqual.equalTo(MathUtils.toEncodedFieldElement(BigInteger.ZERO)));
		Assert.assertThat(encoded2, IsEqual.equalTo(MathUtils.toEncodedFieldElement(BigInteger.ONE)));
	}

	@Test
	public void encodeReturnsCorrectByteArrayIfJthBitOfTiIsSetToOne() {
		for (int i = 0; i < 10; i++) {
			// Arrange:
			final int[] t = new int[10];
			for (int j = 0; j < 24; j++) {
				t[i] = 1 << j;
				final Ed25519FieldElement fieldElement = new Ed25519FieldElement(t);
				final BigInteger b = MathUtils.toBigInteger(t).mod(Ed25519Field.P);

				// Act:
				final Ed25519EncodedFieldElement encoded = fieldElement.encode();

				// Assert:
				Assert.assertThat(encoded, IsEqual.equalTo(MathUtils.toEncodedFieldElement(b)));
			}
		}
	}

	@Test
	public void encodeReturnsCorrectByteArray() {
		final SecureRandom random = new SecureRandom();
		for (int i = 0; i < 10000; i++) {
			// Arrange:
			final int[] t = new int[10];
			for (int j = 0; j < 10; j++) {
				t[j] = random.nextInt(1 << 28) - (1 << 27);
			}
			final Ed25519FieldElement fieldElement = new Ed25519FieldElement(t);
			final BigInteger b = MathUtils.toBigInteger(t);

			// Act:
			final Ed25519EncodedFieldElement encoded = fieldElement.encode();

			// Assert:
			Assert.assertThat(encoded, IsEqual.equalTo(MathUtils.toEncodedFieldElement(b.mod(Ed25519Field.P))));
		}
	}

	// region isNegative

	@Test
	public void isNegativeReturnsCorrectResult() {
		final SecureRandom random = new SecureRandom();
		for (int i = 0; i < 10000; i++) {
			// Arrange:
			final byte[] values = new byte[32];
			random.nextBytes(values);
			values[31] &= 0x7F;
			final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(values);
			final boolean isNegative = MathUtils.toBigInteger(encoded).mod(Ed25519Field.P).mod(new BigInteger("2")).equals(BigInteger.ONE);

			// Assert:
			Assert.assertThat(encoded.isNegative(), IsEqual.equalTo(isNegative));
		}
	}

	// endregion

	// region hashCode / equals

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Ed25519EncodedFieldElement encoded1 = MathUtils.getRandomEncodedFieldElement(32);
		final Ed25519EncodedFieldElement encoded2 = encoded1.decode().encode();
		final Ed25519EncodedFieldElement encoded3 = MathUtils.getRandomEncodedFieldElement(32);
		final Ed25519EncodedFieldElement encoded4 = MathUtils.getRandomEncodedFieldElement(32);

		// Assert:
		Assert.assertThat(encoded1, IsEqual.equalTo(encoded2));
		Assert.assertThat(encoded1, IsNot.not(IsEqual.equalTo(encoded3)));
		Assert.assertThat(encoded1, IsNot.not(IsEqual.equalTo(encoded4)));
		Assert.assertThat(encoded3, IsNot.not(IsEqual.equalTo(encoded4)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Ed25519EncodedFieldElement encoded1 = MathUtils.getRandomEncodedFieldElement(32);
		final Ed25519EncodedFieldElement encoded2 = encoded1.decode().encode();
		final Ed25519EncodedFieldElement encoded3 = MathUtils.getRandomEncodedFieldElement(32);
		final Ed25519EncodedFieldElement encoded4 = MathUtils.getRandomEncodedFieldElement(32);

		// Assert:
		Assert.assertThat(encoded1.hashCode(), IsEqual.equalTo(encoded2.hashCode()));
		Assert.assertThat(encoded1.hashCode(), IsNot.not(IsEqual.equalTo(encoded3.hashCode())));
		Assert.assertThat(encoded1.hashCode(), IsNot.not(IsEqual.equalTo(encoded4.hashCode())));
		Assert.assertThat(encoded3.hashCode(), IsNot.not(IsEqual.equalTo(encoded4.hashCode())));
	}

	// endregion

	//region toString

	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final byte[] bytes = new byte[32];
		for (int i = 0; i < 32; i++) {
			bytes[i] = (byte)(i + 1);
		}
		final Ed25519EncodedFieldElement encoded = new Ed25519EncodedFieldElement(bytes);

		// Act:
		final String encodedAsString = encoded.toString();
		final StringBuilder builder = new StringBuilder();
		for (final byte b : bytes) {
			builder.append(String.format("%02x", b));
		}

		// Assert:
		Assert.assertThat(encodedAsString, IsEqual.equalTo(builder.toString()));
	}

	// endregion
}
