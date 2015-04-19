package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class HexEncoderTest {

	//region getBytes

	@Test
	public void getBytesCanConvertValidStringToByteArray() {
		// Assert:
		assertGetBytesConversion(
				"4e454d465457",
				new byte[] { 0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void getBytesCanConvertValidStringWithOddLengthToByteArray() {
		// Assert:
		assertGetBytesConversion(
				"e454d465457",
				new byte[] { 0x0e, 0x45, 0x4d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void getBytesCanConvertValidStringWithLeadingZerosToByteArray() {
		// Assert:
		assertGetBytesConversion(
				"00000d465457",
				new byte[] { 0x00, 0x00, 0x0d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void getBytesCannotConvertMalformedStringToByteArray() {
		// Act:
		ExceptionAssert.assertThrows(v -> HexEncoder.getBytes("4e454g465457"), IllegalArgumentException.class);
	}

	private static void assertGetBytesConversion(final String input, final byte[] expectedOutput) {
		// Act:
		final byte[] output = HexEncoder.getBytes(input);

		// Assert:
		Assert.assertThat(output, IsEqual.equalTo(expectedOutput));
	}

	//endregion

	//region tryGetBytes

	@Test
	public void tryGetBytesCanConvertValidStringToByteArray() {
		// Assert:
		assertTryGetBytesConversion(
				"4e454d465457",
				new byte[] { 0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void tryGetBytesCanConvertValidStringWithOddLengthToByteArray() {
		// Assert:
		assertTryGetBytesConversion(
				"e454d465457",
				new byte[] { 0x0e, 0x45, 0x4d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void tryGetBytesCanConvertValidStringWithLeadingZerosToByteArray() {
		// Assert:
		assertTryGetBytesConversion(
				"00000d465457",
				new byte[] { 0x00, 0x00, 0x0d, 0x46, 0x54, 0x57 });
	}

	@Test
	public void tryGetBytesCannotConvertMalformedStringToByteArray() {
		// Assert:
		assertTryGetBytesConversion(
				"4e454g465457",
				null);
	}

	private static void assertTryGetBytesConversion(final String input, final byte[] expectedOutput) {
		// Act:
		final byte[] output = HexEncoder.tryGetBytes(input);

		// Assert:
		Assert.assertThat(output, IsEqual.equalTo(expectedOutput));
	}

	//endregion

	//region getString

	@Test
	public void getStringCanConvertBytesToHexString() {
		// Assert:
		assertGetStringConversion(
				new byte[] { 0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57 },
				"4e454d465457");
	}

	@Test
	public void getStringCanConvertBytesWithLeadingZerosToHexString() {
		// Assert:
		assertGetStringConversion(
				new byte[] { 0x00, 0x00, 0x0d, 0x46, 0x54, 0x57 },
				"00000d465457");
	}

	private static void assertGetStringConversion(final byte[] input, final String expectedOutput) {
		// Act:
		final String output = HexEncoder.getString(input);

		// Assert:
		Assert.assertThat(output, IsEqual.equalTo(expectedOutput));
	}

	//endregion
}
