package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.math.BigInteger;

public class ByteUtilsTest {
	@Test
	public void canConvertBytesToLong() {
		// Arrange:
		byte[] data = { 1,2,3,4,5,6,7,8 };
		BigInteger b = new BigInteger(data);

		// Act:
		long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test
	public void canConvertBytesToLongNegative() {
		// Arrange:
		byte[] data = { (byte)0x80,2,3,4,5,6,7,8 };
		BigInteger b = new BigInteger(data);

		// Act:
		long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test
	public void conversionToLongIgnoresExcessiveData() {
		// Arrange:
		byte[] data = { 1,2,3,4,5,6,7,8, 9,9,9 };
		BigInteger b = new BigInteger(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7] });

		// Act:
		long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.longValue()));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void conversionToLongFailsOnDataUnderflow() {
		// Arrange:
		byte[] data = { 1,2,3,4,5,6 };

		// Act:
		ByteUtils.bytesToLong(data);
	}

	@Test
	public void canConvertLongToBytes() {
		// Arrange:
		long data = 0x0807060504030201L;
		BigInteger b = BigInteger.valueOf(data);

		// Act:
		byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.toByteArray()));
	}

	@Test
	public void canConvertLongNegativeToBytes() {
		// Arrange:
		long data = 0x8070605040302010L;
		BigInteger b = BigInteger.valueOf(data);

		// Act:
		byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(b.toByteArray()));
	}

	@Test
	public void canRoundtripLongNegative() {
		// Arrange:
		long input = 0x8070605040302010L;
		byte[] data = ByteUtils.longToBytes(input);

		// Act:
		long result = ByteUtils.bytesToLong(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(result));
	}

	@Test
	public void canRoundtripBytesNegative() {
		// Arrange:
		byte[] input = { (byte)0x80,2,3,4,5,6,7,8 };
		long data = ByteUtils.bytesToLong(input);

		// Act:
		byte[] result = ByteUtils.longToBytes(data);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(result));
	}
}
