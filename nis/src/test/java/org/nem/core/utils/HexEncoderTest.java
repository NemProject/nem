package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class HexEncoderTest {

	@Test
	public void stringCanBeConvertedToByteArray() {
		// Assert:
		Assert.assertThat(
				HexEncoder.getBytes("4e454d465457"),
				IsEqual.equalTo(new byte[] { 0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57 }));
	}

	@Test
	public void stringWithOddLengthCanBeConvertedToByteArray() {
		// Assert:
		Assert.assertThat(
				HexEncoder.getBytes("e454d465457"),
				IsEqual.equalTo(new byte[] { 0x0e, 0x45, 0x4d, 0x46, 0x54, 0x57 }));
	}

	@Test
	public void stringWithLeadingZerosCanBeConvertedToByteArray() {
		// Assert:
		Assert.assertThat(
				HexEncoder.getBytes("00000d465457"),
				IsEqual.equalTo(new byte[] { 0x00, 0x00, 0x0d, 0x46, 0x54, 0x57 }));
	}

	@Test
	public void byteArrayCanBeConvertedToString() {
		// Assert:
		Assert.assertThat(
				HexEncoder.getString(new byte[] { 0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57 }),
				IsEqual.equalTo("4e454d465457"));
	}

	@Test
	public void byteArrayWithLeadingZerosCanBeConvertedToString() {
		// Assert:
		Assert.assertThat(HexEncoder.getString(
				new byte[] { 0x00, 0x00, 0x0d, 0x46, 0x54, 0x57 }),
				IsEqual.equalTo("00000d465457"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void malformedStringCannotBeConvertedToByteArray() {
		// Act:
		HexEncoder.getBytes("4e454g465457");
	}
}
