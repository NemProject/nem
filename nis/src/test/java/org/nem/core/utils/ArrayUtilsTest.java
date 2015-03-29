package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ArrayUtilsTest {

	//region duplicate

	@Test
	public void duplicateIsNotReference() {
		// Arrange:
		final byte[] src = new byte[] { 1, 2, 3, 4 };

		// Act:
		final byte[] result = ArrayUtils.duplicate(src);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsSame.sameInstance(src)));
	}

	@Test
	public void duplicateCanDuplicateEmptyArray() {
		// Assert:
		assertCanDuplicate(new byte[] {});
	}

	@Test
	public void duplicateCanDuplicateNonEmptyArray() {
		// Assert:
		assertCanDuplicate(new byte[] { 1, 2, 3, 4 });
	}

	private static void assertCanDuplicate(final byte[] bytes) {
		// Act:
		final byte[] result = ArrayUtils.duplicate(bytes);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(bytes));
	}

	@Test(expected = NullPointerException.class)
	public void duplicateThrowsExceptionOnNull() {
		// Act:
		ArrayUtils.duplicate(null);
	}

	//endregion

	//region concat

	@Test
	public void concatCanCombineEmptyArrayWithEmptyArray() {
		// Arrange:
		final byte[] lhs = new byte[] {};
		final byte[] rhs = new byte[] {};

		// Act:
		final byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] {}));
	}

	@Test
	public void concatCanCombineEmptyArrayWithNonEmptyArray() {
		// Arrange:
		final byte[] lhs = new byte[] {};
		final byte[] rhs = new byte[] { 12, 4, 6 };

		// Act:
		final byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 12, 4, 6 }));
	}

	@Test
	public void concatCanCombineNonEmptyArrayWithEmptyArray() {
		// Arrange:
		final byte[] lhs = new byte[] { 7, 13 };
		final byte[] rhs = new byte[] {};

		// Act:
		final byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13 }));
	}

	@Test
	public void concatCanCombineNonEmptyArrayWithNonEmptyArray() {
		// Arrange:
		final byte[] lhs = new byte[] { 7, 13 };
		final byte[] rhs = new byte[] { 12, 4, 6 };

		// Act:
		final byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13, 12, 4, 6 }));
	}

	@Test
	public void concatCanCombineMoreThanTwoArrays() {
		// Act:
		final byte[] result = ArrayUtils.concat(new byte[] { 7, 13 }, new byte[] { 12, 4, 6 }, new byte[] { 11, 9 });

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13, 12, 4, 6, 11, 9 }));
	}

	//endregion

	//region split

	@Test(expected = IllegalArgumentException.class)
	public void splitFailsIfSplitIndexIsNegative() {
		// Arrange:
		final byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		ArrayUtils.split(bytes, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void splitFailsIfSplitIndexIsGreaterThanInputLength() {
		// Arrange:
		final byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		ArrayUtils.split(bytes, bytes.length + 1);
	}

	@Test
	public void canSplitEmptyArray() {
		// Arrange:
		final byte[] bytes = new byte[] {};

		// Act:
		final byte[][] parts = ArrayUtils.split(bytes, 0);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] {}));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] {}));
	}

	@Test
	public void canSplitArrayAtBeginning() {
		// Arrange:
		final byte[] bytes = new byte[] { 12, 4, 6 };

		// Act:
		final byte[][] parts = ArrayUtils.split(bytes, 0);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] {}));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { 12, 4, 6 }));
	}

	@Test
	public void canSplitArrayAtEnd() {
		// Arrange:
		final byte[] bytes = new byte[] { 7, 13 };

		// Act:
		final byte[][] parts = ArrayUtils.split(bytes, 2);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { 7, 13 }));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] {}));
	}

	@Test
	public void canSplitArrayAtMiddle() {
		// Arrange:
		final byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		final byte[][] parts = ArrayUtils.split(bytes, 2);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { 7, 13 }));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { 12, 4, 6 }));
	}

	//endregion

	//region toByteArray

	@Test
	public void canConvertPositiveBigIntegerToByteArray() {
		// Act:
		final byte[] bytes = ArrayUtils.toByteArray(new BigInteger("321495", 16), 3);

		// Assert:
		Assert.assertThat(bytes, IsEqual.equalTo(new byte[] { (byte)0x95, 0x14, 0x32 }));
	}

	@Test
	public void canConvertNegativeBigIntegerToByteArray() {
		// Act:
		final byte[] bytes = ArrayUtils.toByteArray(new BigInteger("F21495", 16), 3);

		// Assert:
		Assert.assertThat(bytes, IsEqual.equalTo(new byte[] { (byte)0x95, 0x14, (byte)0xF2 }));
	}

	@Test
	public void canConvertBigIntegerWithLeadingZerosToByteArray() {
		// Act:
		final byte[] bytes = ArrayUtils.toByteArray(new BigInteger("0000A5", 16), 3);

		// Assert:
		Assert.assertThat(bytes, IsEqual.equalTo(new byte[] { (byte)0xA5, 0x00, 0x00 }));
	}

	@Test
	public void byteArrayConversionTruncatesLargeInteger() {
		// Act:
		final byte[] bytes = ArrayUtils.toByteArray(new BigInteger("F122321495", 16), 3);

		// Assert:
		Assert.assertThat(bytes, IsEqual.equalTo(new byte[] { (byte)0x95, 0x14, 0x32 }));
	}

	//endregion

	//region toBigInteger

	@Test
	public void canConvertByteArrayToPositiveBigInteger() {
		// Act:
		final BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0x95, 0x14, 0x32 });

		// Assert:
		Assert.assertThat(new BigInteger("321495", 16), IsEqual.equalTo(result));
	}

	@Test
	public void canConvertByteArrayToNegativeBigInteger() {
		// Act:
		final BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0x95, 0x14, (byte)0xF2 });

		// Assert:
		Assert.assertThat(new BigInteger("F21495", 16), IsEqual.equalTo(result));
	}

	@Test
	public void canConvertByteArrayWithLeadingZerosToBigInteger() {
		// Act:
		final BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0xA5, 0x00, 0x00 });

		// Assert:
		Assert.assertThat(new BigInteger("0000A5", 16), IsEqual.equalTo(result));
	}

	//endregion

	//region isEqual

	@Test
	public void isEqualsReturnsOneForEqualByteArrays() {
		// Arrange:
		final SecureRandom random = new SecureRandom();
		final byte[] bytes1 = new byte[32];
		final byte[] bytes2 = new byte[32];
		for (int i = 0; i < 100; i++) {
			random.nextBytes(bytes1);
			System.arraycopy(bytes1, 0, bytes2, 0, 32);

			// Act:
			final int result = ArrayUtils.isEqualConstantTime(bytes1, bytes2);

			// Assert:
			Assert.assertThat(createMessage(bytes1, bytes2), result, IsEqual.equalTo(1));
		}
	}

	@Test
	public void isEqualsReturnsZeroForUnequalByteArrays() {
		// Arrange:
		final SecureRandom random = new SecureRandom();
		final byte[] bytes1 = new byte[32];
		final byte[] bytes2 = new byte[32];
		random.nextBytes(bytes1);
		for (int i = 0; i < 32; i++) {
			System.arraycopy(bytes1, 0, bytes2, 0, 32);
			bytes2[i] = (byte)(bytes2[i] ^ 0xff);

			// Act:
			final int result = ArrayUtils.isEqualConstantTime(bytes1, bytes2);

			// Assert:
			Assert.assertThat(createMessage(bytes1, bytes2), result, IsEqual.equalTo(0));
		}
	}

	private static String createMessage(final byte[] bytes1, final byte[] bytes2) {
		return String.format(
				"bytes1: %s%sbytes2: %s",
				ByteUtils.toString(bytes1),
				System.lineSeparator(),
				ByteUtils.toString(bytes2));
	}

	//endregion

	//region compare

	@Test
	public void compareReturnsZeroForEmptyArrays() {
		// Assert:
		assertCompareZero(
				new byte[] {},
				new byte[] {});
	}

	@Test
	public void compareReturnsNonZeroForEmptyAndNonEmptyArrays() {
		// Assert:
		assertCompareNonZero(
				new byte[] {},
				new byte[] { 5 });
	}

	@Test
	public void compareReturnsNonZeroForDifferentSizedArrays() {
		// Assert:
		assertCompareNonZero(
				new byte[] { 54 },
				new byte[] { 5, 4 });
	}

	@Test
	public void compareReturnsNonZeroForArraysOfSameLengthWithDifferentPositiveElementValue() {
		// Assert:
		assertCompareNonZero(
				new byte[] { 1, 2, 3, 4, 5 },
				new byte[] { 1, 2, 4, 4, 5 });
	}

	@Test
	public void compareReturnsNonZeroForArraysOfSameLengthWithDifferentNegativeElementValue() {
		// Assert:
		assertCompareNonZero(
				new byte[] { 1, 2, -4, 4, 5 },
				new byte[] { 1, 2, -3, 4, 5 });
	}

	@Test
	public void compareReturnsZeroForArraysOfSameLengthWithEqualElementValues() {
		// Assert:
		assertCompareZero(
				new byte[] { 1, 2, 3, 4, 5 },
				new byte[] { 1, 2, 3, 4, 5 });
	}

	private static void assertCompareZero(final byte[] a, final byte[] b) {
		// Assert:
		Assert.assertThat(ArrayUtils.compare(a, b), IsEqual.equalTo(0));
		Assert.assertThat(ArrayUtils.compare(b, a), IsEqual.equalTo(0));
	}

	private static void assertCompareNonZero(final byte[] smaller, final byte[] larger) {
		// Assert:
		Assert.assertThat(ArrayUtils.compare(smaller, larger), IsEqual.equalTo(-1));
		Assert.assertThat(ArrayUtils.compare(larger, smaller), IsEqual.equalTo(1));
	}

	//endregion

	//region getBit

	@Test
	public void getBitReturnZeroIfBitIsNotSet() {
		Assert.assertThat(ArrayUtils.getBit(new byte[] { 0 }, 0), IsEqual.equalTo(0));
		Assert.assertThat(ArrayUtils.getBit(new byte[] { 1, 2, 3 }, 15), IsEqual.equalTo(0));
	}

	@Test
	public void getBitReturnOneIfBitIsSet() {
		// Assert:
		Assert.assertThat(ArrayUtils.getBit(new byte[] { 8 }, 3), IsEqual.equalTo(1));
		Assert.assertThat(ArrayUtils.getBit(new byte[] { 1, 2, 3 }, 9), IsEqual.equalTo(1));
		Assert.assertThat(ArrayUtils.getBit(new byte[] { 1, 2, 3 }, 16), IsEqual.equalTo(1));
	}

	//endregion
}
