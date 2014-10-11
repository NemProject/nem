package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.math.BigInteger;

public class ByteUtilsTest {
	@Test
	public void canConvertBytesToLong() {
		// Arrange:
		final byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8 };
		final BigInteger b = new BigInteger(data);

		// Act:
		final long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test
	public void canConvertBytesToLongNegative() {
		// Arrange:
		final byte[] data = { (byte)0x80, 2, 3, 4, 5, 6, 7, 8 };
		final BigInteger b = new BigInteger(data);

		// Act:
		final long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test
	public void conversionToLongIgnoresExcessiveData() {
		// Arrange:
		final byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9 };
		final BigInteger b = new BigInteger(new byte[] { data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7] });

		// Act:
		final long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void conversionToLongFailsOnDataUnderflow() {
		// Arrange:
		final byte[] data = { 1, 2, 3, 4, 5, 6 };

		// Act:
		ByteUtils.bytesToLong(data);
	}

	@Test
	public void canConvertLongToBytes() {
		// Arrange:
		final long data = 0x0807060504030201L;
		final BigInteger b = BigInteger.valueOf(data);

		// Act:
		final byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.toByteArray()));
	}

	@Test
	public void canConvertLongNegativeToBytes() {
		// Arrange:
		final long data = 0x8070605040302010L;
		final BigInteger b = BigInteger.valueOf(data);

		// Act:
		final byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.toByteArray()));
	}

	@Test
	public void canRoundtripLongNegative() {
		// Arrange:
		final long input = 0x8070605040302010L;
		final byte[] data = ByteUtils.longToBytes(input);

		// Act:
		final long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(result));
	}

	@Test
	public void canRoundtripBytesNegative() {
		// Arrange:
		final byte[] input = { (byte)0x80, 2, 3, 4, 5, 6, 7, 8 };
		final long data = ByteUtils.bytesToLong(input);

		// Act:
		final byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(result));
	}

	//region isEqual

	@Test
	public void isEqualReturnsOneIfBytesAreEqual() {
		// Assert:
		Assert.assertThat(ByteUtils.isEqual(0, 0), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqual(7, 7), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqual(64, 64), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqual(255, 255), IsEqual.equalTo(1));
	}

	@Test
	public void isEqualReturnsZeroIfBytesAreNotEqual() {
		// Assert:
		Assert.assertThat(ByteUtils.isEqual(0, 1), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqual(7, -7), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqual(64, 63), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqual(254, 255), IsEqual.equalTo(0));
	}

	//endregion

	// region isNegative

	@Test
	public void isNegativeReturnsOneIfByteIsNegative() {
		// Assert:
		Assert.assertThat(ByteUtils.isNegative(-1), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isNegative(-100), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isNegative(-255), IsEqual.equalTo(1));
	}

	@Test
	public void isNegativeReturnsZeroIfByteIsZeroOrPositive() {
		// Assert:
		Assert.assertThat(ByteUtils.isNegative(0), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegative(1), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegative(32), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegative(127), IsEqual.equalTo(0));
	}

	//endregion
}
