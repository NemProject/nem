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

	@Test (expected = IllegalArgumentException.class)
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

	// region mod p arithmetic

	@Test
	public void addReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.add(f2);
			final BigInteger b3 = MathUtils.toBigInteger(f3).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b3, IsEqual.equalTo(b1.add(b2).mod(Ed25519Field.P)));
		}
	}

	@Test
	public void subtractReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.subtract(f2);
			final BigInteger b3 = MathUtils.toBigInteger(f3).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b3, IsEqual.equalTo(b1.subtract(b2).mod(Ed25519Field.P)));
		}
	}

	@Test
	public void negateReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.negate();
			final BigInteger b2 = MathUtils.toBigInteger(f2).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1.negate().mod(Ed25519Field.P)));
		}
	}

	@Test
	public void multiplyReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);
			final BigInteger b2 = MathUtils.toBigInteger(f2);

			// Act:
			final Ed25519FieldElement f3 = f1.multiply(f2);
			final BigInteger b3 = MathUtils.toBigInteger(f3).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b3, IsEqual.equalTo(b1.multiply(b2).mod(Ed25519Field.P)));
		}
	}

	@Test
	public void squareReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.square();
			final BigInteger b2 = MathUtils.toBigInteger(f2).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1.multiply(b1).mod(Ed25519Field.P)));
		}
	}

	@Test
	public void squareAndDoubleReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.squareAndDouble();
			final BigInteger b2 = MathUtils.toBigInteger(f2).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1.multiply(b1).multiply(new BigInteger("2")).mod(Ed25519Field.P)));
		}
	}

	@Test
	public void invertReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.invert();
			final BigInteger b2 = MathUtils.toBigInteger(f2).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1.modInverse(Ed25519Field.P)));
		}
	}

	@Test
	public void pow22523ReturnsCorrectResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final BigInteger b1 = MathUtils.toBigInteger(f1);

			// Act:
			final Ed25519FieldElement f2 = f1.pow22523();
			final BigInteger b2 = MathUtils.toBigInteger(f2).mod(Ed25519Field.P);

			// Assert:
			Assert.assertThat(b2, IsEqual.equalTo(b1.modPow(BigInteger.ONE.shiftLeft(252).subtract(new BigInteger("3")), Ed25519Field.P)));
		}
	}

	// endregion

	// regiondecode

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
		SecureRandom random = new SecureRandom();
		for (int i=0; i<10000; i++) {
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
		SecureRandom random = new SecureRandom();
		for (int i=0; i<10000; i++) {
			// Arrange:
			final int[] t = new int[10];
			for (int j=0; j<10; j++) {
				t[j] = random.nextInt(1 << 28) - (1 << 27);
			}
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
		for (int i=0; i<32; i++) {
			bytes[i] = (byte)(i+1);
		}
		final Ed25519FieldElement f = new Ed25519EncodedFieldElement(bytes).decode();

		// Act:
		final String fAsString = f.toString();
		final StringBuilder builder = new StringBuilder();
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}

		// Assert:
		Assert.assertThat(fAsString, IsEqual.equalTo(builder.toString()));
	}

	// endregion
}
