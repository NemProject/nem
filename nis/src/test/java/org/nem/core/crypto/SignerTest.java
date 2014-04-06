package org.nem.core.crypto;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

public class SignerTest {
	@Test
	public void sha3HmacUsedInSignerCreateEcdsaSigner() throws DecoderException {
		// Arrange
		SHA3Digest sha3 = new SHA3Digest(256);
		HMac hMac = new HMac(sha3);

		byte[] key = { 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb };
		byte[] data = { 0x48, 0x69, 0x20, 0x54, 0x68, 0x65, 0x72, 0x65 };

		// Act:
		hMac.init(new KeyParameter(key));
		hMac.update(data, 0, data.length);
		byte[] result = new byte[32];
		hMac.doFinal(result, 0);

		// Assert:
		byte[] expectedResult = HexEncoder.getBytes("9663d10c73ee294054dc9faf95647cb99731d12210ff7075fb3d3395abfb9821");

		Assert.assertArrayEquals(result, expectedResult);
	}

	@Test
	public void signedDataCanBeVerified() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Signer signer = new Signer(kp);
		byte[] input = Utils.generateRandomBytes();

		// Act:
		Signature signature = signer.sign(input);

		// Assert:
		Assert.assertThat(signer.verify(input, signature), IsEqual.equalTo(true));
	}

	@Test
	public void dataSignedWithKeyPairCannotBeVerifiedWithDifferentKeyPair() {
		// Arrange:
		KeyPair kp1 = new KeyPair();
		KeyPair kp2 = new KeyPair();
		Signer signer1 = new Signer(kp1);
		Signer signer2 = new Signer(kp2);
		byte[] input = Utils.generateRandomBytes();

		// Act:
		Signature signature1 = signer1.sign(input);
		Signature signature2 = signer2.sign(input);

		// Assert:
		Assert.assertThat(signer1.verify(input, signature1), IsEqual.equalTo(true));
		Assert.assertThat(signer1.verify(input, signature2), IsEqual.equalTo(false));
		Assert.assertThat(signer2.verify(input, signature1), IsEqual.equalTo(false));
		Assert.assertThat(signer2.verify(input, signature2), IsEqual.equalTo(true));
	}

	@Test
	public void signaturesAreDeterministic() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Signer signer = new Signer(kp);
		byte[] input = Utils.generateRandomBytes();

		// Act:
		Signature signature1 = signer.sign(input);
		Signature signature2 = signer.sign(input);

		// Assert:
		Assert.assertThat(signature1, IsEqual.equalTo(signature2));
	}

	@Test
	public void verifyReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Signer signer = new Signer(kp);
		byte[] input = Utils.generateRandomBytes();

		// Act:
		Signature signature = signer.sign(input);
		BigInteger nonCanonicalS = Curves.secp256k1().getParams().getN().subtract(signature.getS());
		Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

		// Assert:
		Assert.assertThat(signer.verify(input, nonCanonicalSignature), IsEqual.equalTo(false));
	}
}
