package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.*;

import java.math.BigInteger;

public class SignerTest {

	@Test
	public void signerProducesCorrectSignatureUsing256bitSha3() {
		// Arrange
		final KeyPair keyPair = new KeyPair(new PrivateKey(BigInteger.valueOf(1L)));
		final Signer signer = new Signer(keyPair);

		// Act:
		final Signature signature = signer.sign(StringEncoder.getBytes("NEM"));

		// Assert:
		final String expectedSignature = "01485191de9fa79887300a2543e2ae5860c744863c380e9ccd2b0c62d768e61b68e3c1f8e8fe4206a4b598f512b5944a43cf8dac03fc871c2ed7d2b927643852";
		Assert.assertThat(HexEncoder.getString(signature.getBytes()), IsEqual.equalTo(expectedSignature));
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
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getS());
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
				CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getR()),
				signature.getS());

		// Assert:
		Assert.assertThat(signer.verify(input, signature2), IsEqual.equalTo(false));
	}
}
