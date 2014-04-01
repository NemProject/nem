package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsSame;
import org.junit.*;

import java.math.BigInteger;
import java.security.InvalidParameterException;

public class ArrayUtilsTest {
	//region duplicate
	@Test
	public void duplicateIsNotReference() {
		// Arrange:
		byte[] src = new byte[] { 1, 2, 3, 4 };

		// Act:
		byte[] result = ArrayUtils.duplicate(src);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsSame.sameInstance(src)));
	}

	@Test
	public void duplicateIsEqual() {
		// Arrange:
		byte[] src1 = new byte[] { 1, 2, 3, 4 };
		byte[] src2 = new byte[] { };

		// Act:
		byte[] result1 = ArrayUtils.duplicate(src1);
		byte[] result2 = ArrayUtils.duplicate(src2);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(src1));
		Assert.assertThat(result2, IsEqual.equalTo(src2));
	}

	@Test(expected = NullPointerException.class)
	public void duplicateThrowsExceptionOnNull() {
		// Arrange:
		byte[] src = null;

		// Act:
		byte[] result = ArrayUtils.duplicate(src);
	}
	//endregion duplicate

	//region concat

	@Test
	public void concatCanCombineEmptyArrayWithEmptyArray() {
		// Arrange:
		byte[] lhs = new byte[] { };
		byte[] rhs = new byte[] { };

		// Act:
		byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { }));
	}

	@Test
	public void concatCanCombineEmptyArrayWithNonEmptyArray() {
		// Arrange:
		byte[] lhs = new byte[] { };
		byte[] rhs = new byte[] { 12, 4, 6 };

		// Act:
		byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 12, 4, 6 }));
	}

	@Test
	public void concatCanCombineNonEmptyArrayWithEmptyArray() {
		// Arrange:
		byte[] lhs = new byte[] { 7, 13 };
		byte[] rhs = new byte[] { };

		// Act:
		byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13 }));
	}

	@Test
	public void concatCanCombineNonEmptyArrayWithNonEmptyArray() {
		// Arrange:
		byte[] lhs = new byte[] { 7, 13 };
		byte[] rhs = new byte[] { 12, 4, 6 };

		// Act:
		byte[] result = ArrayUtils.concat(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13, 12, 4, 6 }));
	}

	//endregion

	//region split

	@Test(expected = InvalidParameterException.class)
	public void splitFailsIfSplitIndexIsNegative() {
		// Arrange:
		byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		ArrayUtils.split(bytes, -1);
	}

	@Test(expected = InvalidParameterException.class)
	public void splitFailsIfSplitIndexIsGreaterThanInputLength() {
		// Arrange:
		byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		ArrayUtils.split(bytes, bytes.length + 1);
	}

	@Test
	public void canSplitEmptyArray() {
		// Arrange:
		byte[] bytes = new byte[] { };

		// Act:
		byte[][] parts = ArrayUtils.split(bytes, 0);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { }));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { }));
	}

	@Test
	public void canSplitArrayAtBeginning() {
		// Arrange:
		byte[] bytes = new byte[] { 12, 4, 6 };

		// Act:
		byte[][] parts = ArrayUtils.split(bytes, 0);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { }));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { 12, 4, 6 }));
	}

	@Test
	public void canSplitArrayAtEnd() {
		// Arrange:
		byte[] bytes = new byte[] { 7, 13 };

		// Act:
		byte[][] parts = ArrayUtils.split(bytes, 2);

		// Assert:
		Assert.assertThat(parts.length, IsEqual.equalTo(2));
		Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { 7, 13 }));
		Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { }));
	}

	@Test
	public void canSplitArrayAtMiddle() {
		// Arrange:
		byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

		// Act:
		byte[][] parts = ArrayUtils.split(bytes, 2);

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
		BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0x95, 0x14, 0x32 });

		// Assert:
		Assert.assertThat(new BigInteger("321495", 16), IsEqual.equalTo(result));
	}

	@Test
	public void canConvertByteArrayToNegativeBigInteger() {
		// Act:
		BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0x95, 0x14, (byte)0xF2 });

		// Assert:
		Assert.assertThat(new BigInteger("F21495", 16), IsEqual.equalTo(result));
	}

	@Test
	public void canConvertByteArrayWithLeadingZerosToBigInteger() {
		// Act:
		BigInteger result = ArrayUtils.toBigInteger(new byte[] { (byte)0xA5, 0x00, 0x00 });

		// Assert:
		Assert.assertThat(new BigInteger("0000A5", 16), IsEqual.equalTo(result));
	}

	//endregion
}
