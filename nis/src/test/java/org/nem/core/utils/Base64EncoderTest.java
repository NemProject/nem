package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.serialization.JsonSerializer;

public class Base64EncoderTest {

	private static final byte[] ENCODED_SIGMA_BYTES = new byte[] {
			0x53, 0x69, 0x67, 0x6D, 0x61
	};

	private static final byte[] ENCODED_CURRENCY_SYMBOLS_BYTES = new byte[] {
			0x24, (byte)0xC2, (byte)0xA2, (byte)0xE2, (byte)0x82, (byte)0xAC
	};

	@Test
	public void stringCanBeConvertedToByteArray() {
		// Assert:
		Assert.assertThat(Base64Encoder.getBytes("U2lnbWE="), IsEqual.equalTo(ENCODED_SIGMA_BYTES));
		Assert.assertThat(Base64Encoder.getBytes("JMKi4oKs"), IsEqual.equalTo(ENCODED_CURRENCY_SYMBOLS_BYTES));
	}

	@Test
	public void byteArrayCanBeConvertedToString() {
		// Assert:
		Assert.assertThat(Base64Encoder.getString(ENCODED_SIGMA_BYTES), IsEqual.equalTo("U2lnbWE="));
		Assert.assertThat(Base64Encoder.getString(ENCODED_CURRENCY_SYMBOLS_BYTES), IsEqual.equalTo("JMKi4oKs"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void malformedStringCannotBeDecoded() {
		// Act:
		Base64Encoder.getBytes("BAD STRING)(*&^%$#@!");
	}

	@Test
	public void stringCanContainPaddingAndWhitespace() {
		// Assert:
		Assert.assertThat(Base64Encoder.getBytes("  U2lnbWE=  "), IsEqual.equalTo(ENCODED_SIGMA_BYTES));
	}
	
	@Test
	public void foo() {
		final String[] privKeys = new String[] {
				"308b13a47a241084aac7112aae5552a8227477ab6c6e2b5b3a996c9cd60da387",
				"00c9658e78146a62ee0bc222a25ca79667b7a7e5225b839fce1d4aa20e64aecf1a",
				"37c5c01c109d58a0d5f7fd48ec11507866808dddf72d4b5baeae6d3c7f506150",
				"009b815cc4f967bae9fc08cff7b5087967af5b2c6d8a07a45eb39488fe8a38d079",
				"38caf638f4f193fcc689b3eeb3c73849ffe68c12302f24138582d7d8d0cdf918",
				"3bb3ac743674dc021589c84df808c2bdd5b3a561dcdf6559e602667ef9417eaa",
				"5b193f29962d9a821479ef335c6a24b2e83bec9942c01d46333f9aa1b3cf2a64",
				"00924d2e6740e223139e89a416ee9cad110c68744bdf27788c4ffe0e00447bf6db"
			};
		final String[] pubKeys = new String[] {
				"02f25538f7fbdb0dbe7d3363be67f9edac8033c777cf3fc10ededae1a990c5459f",
				"03c55bd250e56c292ed4c898b0883676313283251d21b6a9099bb989db99d736d2",
				"036ccaeb7c39125a5d498e48a34a0811a4e8321c0eb37b9e85128823b37142ce48",
				"02bb032b4eaf976090d00776259602b09baebf06908d7855372d2a4eb1db21042a",
				"0350f94f8c3a04a4f47356ba749b74418a55511d88a56d180998130d8c26b28bfd",
				"031f7b6c1c446a0e9c6f5b76f64043e4b05c6a91e3820624d930e423d0cc644567",
				"037dc7cecbab7d727fc9a248b3be707fe3c1945dbd49712e132d39f9322bd1849d",
				"0270e1455c836d600f5b346e1b23bd87320ddebd4d53f38233b19fb05122532544",
				"02bffdf8ee526311b329a00a82c8cd866f6ba5cda17c165899b25f5c4f4a62beee"
			};
		for (final String privKey : privKeys) {
			System.out.println(privKey);
			final PrivateKey key = PrivateKey.fromHexString(privKey);
			final JsonSerializer serializer = new JsonSerializer();
			serializer.writeObject("key", key);
			System.out.println(serializer.getObject().get("key"));
		}
		System.out.println("");
		for (final String pubKey : pubKeys) {
			System.out.println(pubKey);
			final PrivateKey key = PrivateKey.fromHexString(pubKey);
			final JsonSerializer serializer = new JsonSerializer();
			serializer.writeObject("key", key);
			System.out.println(serializer.getObject().get("key"));
		}
	}
}
