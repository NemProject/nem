package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;

public class HexEncoderTest {

	private static final byte[] ENCODED_SIGMA_BYTES = new byte[] {
			0x4e, 0x45, 0x4d, 0x46, 0x54, 0x57
	};

	@Test
	public void stringCanBeConvertedToByteArray() throws Exception {
		// Assert:
		Assert.assertThat(HexEncoder.getBytes("4e454d465457"), IsEqual.equalTo(ENCODED_SIGMA_BYTES));
	}

	@Test
	public void byteArrayCanBeConvertedToString() {
		// Assert:
		Assert.assertThat(HexEncoder.getString(ENCODED_SIGMA_BYTES), IsEqual.equalTo("4e454d465457"));
	}

	@Test(expected = EncodingException.class)
	public void malformedStringCannotBeConvertedToByteArray() {
		// Act:
		HexEncoder.getBytes("4e454g465457");
	}
}
