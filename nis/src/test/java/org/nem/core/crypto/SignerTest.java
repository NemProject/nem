package org.nem.core.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

public class SignerTest {

	// TODO: this test doesn't seem to be doing anything with Signer
	// TODO: we should probably add at least one test validating the produced signature exactly

	@Test
	public void sha3HmacUsedInSignerCreateEcdsaSigner() {
		// Arrange
		final SHA3Digest sha3 = new SHA3Digest(256);
		final HMac hMac = new HMac(sha3);

		final byte[] key = { 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb, 0xb };
		final byte[] data = { 0x48, 0x69, 0x20, 0x54, 0x68, 0x65, 0x72, 0x65 };

		// Act:
		hMac.init(new KeyParameter(key));
		hMac.update(data, 0, data.length);
		final byte[] result = new byte[32];
		hMac.doFinal(result, 0);

		// Assert:
		final byte[] expectedResult = HexEncoder.getBytes(
				"9663d10c73ee294054dc9faf95647cb99731d12210ff7075fb3d3395abfb9821");

		Assert.assertArrayEquals(result, expectedResult);
	}

	@Test
	public void signedDataCanBeVerified() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Signer signer = new Signer(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = signer.sign(input);

		// Assert:
		Assert.assertThat(signer.verify(input, signature), IsEqual.equalTo(true));
	}

	@Test
	public void dataSignedWithKeyPairCannotBeVerifiedWithDifferentKeyPair() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();
		final KeyPair kp2 = new KeyPair();
		final Signer signer1 = new Signer(kp1);
		final Signer signer2 = new Signer(kp2);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature1 = signer1.sign(input);
		final Signature signature2 = signer2.sign(input);

		// Assert:
		Assert.assertThat(signer1.verify(input, signature1), IsEqual.equalTo(true));
		Assert.assertThat(signer1.verify(input, signature2), IsEqual.equalTo(false));
		Assert.assertThat(signer2.verify(input, signature1), IsEqual.equalTo(false));
		Assert.assertThat(signer2.verify(input, signature2), IsEqual.equalTo(true));
	}

	@Test
	public void signaturesAreDeterministic() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Signer signer = new Signer(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature1 = signer.sign(input);
		final Signature signature2 = signer.sign(input);

		// Assert:
		Assert.assertThat(signature1, IsEqual.equalTo(signature2));
	}

	@Test
	public void verifyReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Signer signer = new Signer(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = signer.sign(input);
		final BigInteger nonCanonicalS = Curves.secp256k1().getParams().getN().subtract(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

		// Assert:
		Assert.assertThat(signer.verify(input, nonCanonicalSignature), IsEqual.equalTo(false));
	}

	@Test(expected = CryptoException.class)
	public void cannotSignPayloadWithoutPrivateKey() {
		// Arrange:
		final KeyPair kp = new KeyPair(Utils.generateRandomPublicKey());
		final Signer signer = new Signer(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		signer.sign(input);
	}

	@Test
	public void replacingRWithGroupOrderMinusRInSignatureRuinsSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Signer signer = new Signer(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = signer.sign(input);
		final Signature signature2 = new Signature(
				Curves.secp256k1().getParams().getN().subtract(signature.getR()),
				signature.getS());

		// Assert:
		Assert.assertThat(signer.verify(input, signature2), IsEqual.equalTo(false));
	}
}
