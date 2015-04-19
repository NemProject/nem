package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class StringEncoderTest {

	private static final byte[] ENCODED_SIGMA_BYTES = new byte[] {
			0x53, 0x69, 0x67, 0x6D, 0x61
	};

	private static final byte[] ENCODED_CURRENCY_SYMBOLS_BYTES = new byte[] {
			0x24, (byte)0xC2, (byte)0xA2, (byte)0xE2, (byte)0x82, (byte)0xAC
	};

	@Test
	public void stringCanBeConvertedToByteArray() {
		// Assert:
		Assert.assertThat(StringEncoder.getBytes("Sigma"), IsEqual.equalTo(ENCODED_SIGMA_BYTES));
		Assert.assertThat(StringEncoder.getBytes("$¢€"), IsEqual.equalTo(ENCODED_CURRENCY_SYMBOLS_BYTES));
	}

	@Test
	public void byteArrayCanBeConvertedToString() {
		// Assert:
		Assert.assertThat(StringEncoder.getString(ENCODED_SIGMA_BYTES), IsEqual.equalTo("Sigma"));
		Assert.assertThat(StringEncoder.getString(ENCODED_CURRENCY_SYMBOLS_BYTES), IsEqual.equalTo("$¢€"));
	}
}
