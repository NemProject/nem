package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class ByteUtilsTest {

	//region bytesToLong / longToBytes

	// region bytesToLong

	@Test
	public void canConvertBytesToLong() {
		// Assert:
		assertBytesToLongConversion(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 0x0102030405060708L);
	}

	@Test
	public void canConvertBytesToLongNegative() {
		// Assert:
		assertBytesToLongConversion(new byte[] { (byte)0x80, 2, 3, 4, 5, 6, 7, 8 }, 0x8002030405060708L);
	}

	@Test
	public void conversionToLongIgnoresExcessiveData() {
		// Assert:
		assertBytesToLongConversion(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9 }, 0x0102030405060708L);
	}

	private static void assertBytesToLongConversion(final byte[] input, final long expected) {
		// Act:
		final long result = ByteUtils.bytesToLong(input);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expected));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void conversionToLongFailsOnDataUnderflow() {
		// Arrange:
		final byte[] data = { 1, 2, 3, 4, 5, 6 };

		// Act:
		ByteUtils.bytesToLong(data);
	}

	//endregion

	//region longToBytes

	@Test
	public void canConvertLongToBytes() {
		// Assert:
		assertLongToBytesConversion(0x0807060504030201L, new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 });
	}

	@Test
	public void canConvertLongNegativeToBytes() {
		// Arrange:
		assertLongToBytesConversion(0x8070605040302010L, new byte[] { (byte)0x80, 0x70, 0x60, 0x50, 0x40, 0x30, 0x20, 0x10 });
	}

	private static void assertLongToBytesConversion(final long input, final byte[] expected) {
		// Act:
		final byte[] result = ByteUtils.longToBytes(input);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expected));
	}

	//endregion

	//region roundtrip

	@Test
	public void canRoundtripLongViaBytes() {
		// Arrange:
		final long input = 0x8070605040302010L;

		// Act:
		final long result = ByteUtils.bytesToLong(ByteUtils.longToBytes(input));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(input));
	}

	@Test
	public void canRoundtripBytesViaLong() {
		// Arrange:
		final byte[] input = { (byte)0x80, 2, 3, 4, 5, 6, 7, 8 };

		// Act:
		final byte[] result = ByteUtils.longToBytes(ByteUtils.bytesToLong(input));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(input));
	}

	//endregion

	//endregion

	//region bytesToInt / intToBytes

	// region bytesToInt

	@Test
	public void canConvertBytesToInt() {
		// Assert:
		assertBytesToIntConversion(new byte[] { 1, 2, 3, 4 }, 0x01020304);
	}

	@Test
	public void canConvertBytesToIntNegative() {
		// Assert:
		assertBytesToIntConversion(new byte[] { (byte)0x80, 2, 3, 4 }, 0x80020304);
	}

	@Test
	public void conversionToIntIgnoresExcessiveData() {
		// Assert:
		assertBytesToIntConversion(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9 }, 0x01020304);
	}

	private static void assertBytesToIntConversion(final byte[] input, final int expected) {
		// Act:
		final int result = ByteUtils.bytesToInt(input);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expected));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void conversionToIntFailsOnDataUnderflow() {
		// Arrange:
		final byte[] data = { 1, 2, 3 };

		// Act:
		ByteUtils.bytesToInt(data);
	}

	//endregion

	//region intToBytes

	@Test
	public void canConvertIntToBytes() {
		// Assert:
		assertIntToBytesConversion(0x08070605, new byte[] { 8, 7, 6, 5 });
	}

	@Test
	public void canConvertIntNegativeToBytes() {
		// Arrange:
		assertIntToBytesConversion(0x80706050, new byte[] { (byte)0x80, 0x70, 0x60, 0x50 });
	}

	private static void assertIntToBytesConversion(final int input, final byte[] expected) {
		// Act:
		final byte[] result = ByteUtils.intToBytes(input);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expected));
	}

	//endregion

	//region roundtrip

	@Test
	public void canRoundtripIntViaBytes() {
		// Arrange:
		final int input = 0x80706050;

		// Act:
		final int result = ByteUtils.bytesToInt(ByteUtils.intToBytes(input));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(input));
	}

	@Test
	public void canRoundtripBytesViaInt() {
		// Arrange:
		final byte[] input = { (byte)0x80, 2, 3, 4 };

		// Act:
		final byte[] result = ByteUtils.intToBytes(ByteUtils.bytesToInt(input));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(input));
	}

	//endregion

	//endregion

	//region isEqual

	@Test
	public void isEqualReturnsOneIfBytesAreEqual() {
		// Assert:
		Assert.assertThat(ByteUtils.isEqualConstantTime(0, 0), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqualConstantTime(7, 7), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqualConstantTime(64, 64), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isEqualConstantTime(255, 255), IsEqual.equalTo(1));
	}

	@Test
	public void isEqualReturnsOneIfLoBytesAreEqualButHiBytesAreNot() {
		// Assert:
		Assert.assertThat(ByteUtils.isEqualConstantTime(75 + 256, 75 + 256 * 2), IsEqual.equalTo(1));
	}

	@Test
	public void isEqualReturnsZeroIfBytesAreNotEqual() {
		// Assert:
		Assert.assertThat(ByteUtils.isEqualConstantTime(0, 1), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqualConstantTime(7, -7), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqualConstantTime(64, 63), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isEqualConstantTime(254, 255), IsEqual.equalTo(0));
	}

	//endregion

	// region isNegative

	@Test
	public void isNegativeReturnsOneIfByteIsNegative() {
		// Assert:
		Assert.assertThat(ByteUtils.isNegativeConstantTime(-1), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isNegativeConstantTime(-100), IsEqual.equalTo(1));
		Assert.assertThat(ByteUtils.isNegativeConstantTime(-255), IsEqual.equalTo(1));
	}

	@Test
	public void isNegativeReturnsZeroIfByteIsZeroOrPositive() {
		// Assert:
		Assert.assertThat(ByteUtils.isNegativeConstantTime(0), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegativeConstantTime(1), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegativeConstantTime(32), IsEqual.equalTo(0));
		Assert.assertThat(ByteUtils.isNegativeConstantTime(127), IsEqual.equalTo(0));
	}

	//endregion

	//region toString

	@Test
	public void toStringCreatesCorrectRepresentationForEmptyBytes() {
		// Act:
		final String result = ByteUtils.toString(new byte[] {});

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo("{ }"));
	}

	@Test
	public void toStringCreatesCorrectRepresentationForNonEmptyBytes() {
		// Act:
		final String result = ByteUtils.toString(new byte[] { 0x12, (byte)0x8A, 0x00, 0x07 });

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo("{ 12 8A 00 07 }"));
	}

	//endregion
}
