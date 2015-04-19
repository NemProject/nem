package org.nem.core.crypto.ed25519.arithmetic;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Tests rely on the BigInteger class.
 */
public class Ed25519FieldElementTest {

	// region constructor

	@Test
	public void canCreateFieldElementFromArrayWithCorrectLength() {
		// Assert:
		new Ed25519FieldElement(new int[10]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateFieldElementFromArrayWithIncorrectLength() {
		// Assert:
		new Ed25519FieldElement(new int[9]);
	}

	// endregion

	// region isNonZero

	@Test
	public void isNonZeroReturnsFalseIfFieldElementIsZero() {
		// Act:
		final Ed25519FieldElement f = new Ed25519FieldElement(new int[10]);

		// Assert:
		Assert.assertThat(f.isNonZero(), IsEqual.equalTo(false));
	}

	@Test
	public void isNonZeroReturnsTrueIfFieldElementIsNonZero() {
		// Act:
		final int[] t = new int[10];
		t[0] = 5;
		final Ed25519FieldElement f = new Ed25519FieldElement(t);

		// Assert:
		Assert.assertThat(f.isNonZero(), IsEqual.equalTo(true));
	}

	// endregion

	// region getRaw

	@Test
	public void getRawReturnsUnderlyingArray() {
		// Act:
		final int[] values = new int[10];
		values[0] = 5;
		values[6] = 15;
		values[8] = -67;
		final Ed25519FieldElement f = new Ed25519FieldElement(values);

		// Assert:
		Assert.assertThat(values, IsEqual.equalTo(f.getRaw()));
	}

	// endregion

	// region mod p arithmetic

	@Test
	public void addReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.add(f2);

			// Assert:
			assertEquals(f3, b1.add(b2));
		}
	}

	@Test
	public void subtractReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.subtract(f2);

			// Assert:
			assertEquals(f3, b1.subtract(b2));
		}
	}

	@Test
	public void negateReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.negate();

			// Assert:
			assertEquals(f2, b1.negate());
		}
	}

	@Test
	public void multiplyReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.multiply(f2);

			// Assert:
			assertEquals(f3, b1.multiply(b2));
		}
	}

	@Test
	public void squareReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.square();

			// Assert:
			assertEquals(f2, b1.multiply(b1));
		}
	}

	@Test
	public void squareAndDoubleReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.squareAndDouble();

			// Assert:
			assertEquals(f2, b1.multiply(b1).multiply(new BigInteger("2")));
		}
	}

	@Test
	public void invertReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.invert();

			// Assert:
			assertEquals(f2, b1.modInverse(Ed25519Field.P));
		}
	}

	@Test
	public void sqrtReturnsCorrectResult() {
		for (int i = 0; i < 1000; i++) {
			// Arrange:
			final Ed25519FieldElement u = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement uSquare = u.square();
			final Ed25519FieldElement v = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement vSquare = v.square();
			final Ed25519FieldElement fraction = u.multiply(v.invert());

			// Act:
			final Ed25519FieldElement sqrt = Ed25519FieldElement.sqrt(uSquare, vSquare);

			// Assert:
			// (u / v)^4 == (sqrt(u^2 / v^2))^4.
			Assert.assertThat(fraction.square().square(), IsEqual.equalTo(sqrt.square().square()));

			// (u / v) == +-1 * sqrt(u^2 / v^2) or (u / v) == +-i * sqrt(u^2 / v^2)
			Assert.assertThat(differsOnlyByAFactorOfAFourthRootOfOne(fraction, sqrt), IsEqual.equalTo(true));
		}
	}

	private static boolean differsOnlyByAFactorOfAFourthRootOfOne(final Ed25519FieldElement x, final Ed25519FieldElement root) {
		final Ed25519FieldElement rootTimesI = root.multiply(Ed25519Field.I);
		return x.equals(root) ||
				x.equals(root.negate()) ||
				x.equals(rootTimesI) ||
				x.equals(rootTimesI.negate());
	}

	private static void assertEquals(final Ed25519FieldElement f, final BigInteger b) {
		final BigInteger b2 = MathUtils.toBigInteger(f);
		Assert.assertThat(b2.mod(Ed25519Field.P), IsEqual.equalTo(b.mod(Ed25519Field.P)));
	}

	// endregion

	// region decode

	@Test
	public void decodeReturnsCorrectFieldElementForSimpleByteArrays() {
		// Arrange:
		final Ed25519EncodedFieldElement encoded1 = MathUtils.toEncodedFieldElement(BigInteger.ZERO);
		final Ed25519EncodedFieldElement encoded2 = MathUtils.toEncodedFieldElement(BigInteger.ONE);

		// Act:
		final Ed25519FieldElement f1 = encoded1.decode();
		final Ed25519FieldElement f2 = encoded2.decode();
		final BigInteger b1 = MathUtils.toBigInteger(f1);
		final BigInteger b2 = MathUtils.toBigInteger(f2);

		// Assert:
		Assert.assertThat(b1, IsEqual.equalTo(BigInteger.ZERO));
		Assert.assertThat(b2, IsEqual.equalTo(BigInteger.ONE));
	}

	@Test
	public void decodeReturnsCorrectFieldElement() {
		final SecureRandom random = new SecureRandom();
		for (int i = 0; i < 10000; i++) {
			// Arrange:
			final byte[] bytes = new byte[32];
			random.nextBytes(bytes);
			bytes[31] = (byte)(bytes[31] & 0x7f);
			final BigInteger b1 = MathUtils.toBigInteger(bytes);

			// Act:
			final Ed25519FieldElement f = new Ed25519EncodedFieldElement(bytes).decode();
			final BigInteger b2 = MathUtils.toBigInteger(f.getRaw()).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1));
		}
	}

	// endregion

	// region isNegative

	@Test
	public void isNegativeReturnsCorrectResult() {
		final SecureRandom random = new SecureRandom();
		for (int i = 0; i < 10000; i++) {
			// Arrange:
			final int[] t = new int[10];
			for (int j = 0; j < 10; j++) {
				t[j] = random.nextInt(1 << 28) - (1 << 27);
			}

			// odd numbers are negative
			final boolean isNegative = MathUtils.toBigInteger(t).mod(Ed25519Field.P).mod(new BigInteger("2")).equals(BigInteger.ONE);
			final Ed25519FieldElement f = new Ed25519FieldElement(t);

			// Assert:
			Assert.assertThat(f.isNegative(), IsEqual.equalTo(isNegative));
		}
	}

	// endregion

	// region hashCode / equals

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
		final Ed25519FieldElement f2 = f1.encode().decode();
		final Ed25519FieldElement f3 = MathUtils.getRandomFieldElement();
		final Ed25519FieldElement f4 = MathUtils.getRandomFieldElement();

		// Assert:
		Assert.assertThat(f1, IsEqual.equalTo(f2));
		Assert.assertThat(f1, IsNot.not(IsEqual.equalTo(f3)));
		Assert.assertThat(f1, IsNot.not(IsEqual.equalTo(f4)));
		Assert.assertThat(f3, IsNot.not(IsEqual.equalTo(f4)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
		final Ed25519FieldElement f2 = f1.encode().decode();
		final Ed25519FieldElement f3 = MathUtils.getRandomFieldElement();
		final Ed25519FieldElement f4 = MathUtils.getRandomFieldElement();

		// Assert:
		Assert.assertThat(f1.hashCode(), IsEqual.equalTo(f2.hashCode()));
		Assert.assertThat(f1.hashCode(), IsNot.not(IsEqual.equalTo(f3.hashCode())));
		Assert.assertThat(f1.hashCode(), IsNot.not(IsEqual.equalTo(f4.hashCode())));
		Assert.assertThat(f3.hashCode(), IsNot.not(IsEqual.equalTo(f4.hashCode())));
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
		final Ed25519FieldElement f = new Ed25519EncodedFieldElement(bytes).decode();

		// Act:
		final String fAsString = f.toString();
		final StringBuilder builder = new StringBuilder();
		for (final byte b : bytes) {
			builder.append(String.format("%02x", b));
		}

		// Assert:
		Assert.assertThat(fAsString, IsEqual.equalTo(builder.toString()));
	}

	// endregion
}
