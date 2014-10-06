package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.MathUtils;

import java.math.BigInteger;
import java.util.logging.Logger;

public class Ed25519DsaSignerTest extends DsaSignerTest {

	private static final Logger LOGGER = Logger.getLogger(Ed25519DsaSignerTest.class.getName());

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().add(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(nonCanonicalSignature), IsEqual.equalTo(false));
	}

	@Test
	public void makeCanonicalMakesNonCanonicalSignatureCanonical() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().add(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);
		Assert.assertThat(dsaSigner.isCanonicalSignature(nonCanonicalSignature), IsEqual.equalTo(false));
		final Signature canonicalSignature = dsaSigner.makeSignatureCanonical(nonCanonicalSignature);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(canonicalSignature), IsEqual.equalTo(true));
	}

	/*@Test
	public void signReturnsExpectedSignature() {
		// Arrange:
		initCryptoEngine();
		final BigInteger k = ArrayUtils.toBigInteger(HexEncoder.getBytes("98a5e3a36e67aaba89888bf093de1ad963e774013b3902bfab356d8b90178a63"));
		final PrivateKey privateKey = new PrivateKey(k);
		final PublicKey publicKey = MathUtils.derivePublicKey(privateKey);
		final KeyPair keyPair = new KeyPair(privateKey, publicKey);
		final DsaSigner dsaSigner = getDsaSigner(keyPair);
		//final byte[] input = HexEncoder.getBytes("b4a8f381e70e7a");
		final byte[] input = HexEncoder.getBytes("b4a8f381e70e7a11");

		// Act:
		final Signature signature1 = dsaSigner.sign(input);
		final Signature signature2 = MathUtils.sign(keyPair, input);

		// Assert:
		Assert.assertThat(signature1, IsEqual.equalTo(signature2));
		Assert.assertThat(dsaSigner.verify(input, signature1), IsEqual.equalTo(true));
	}*/

	@Test
	public void signReturnsExpectedSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair keyPair = new KeyPair();
		for (int i=0; i<20; i++) {
			final DsaSigner dsaSigner = getDsaSigner(keyPair);
			final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

			// Act:
			final Signature signature1 = dsaSigner.sign(input);
			final Signature signature2 = MathUtils.sign(keyPair, input);

			// Assert:
			Assert.assertThat(signature1, IsEqual.equalTo(signature2));
		}
	}

	@Test
	public void signReturnsVerifiableSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair keyPair = new KeyPair();
		for (int i=0; i<20; i++) {
			final DsaSigner dsaSigner = getDsaSigner(keyPair);
			final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

			// Act:
			final Signature signature1 = dsaSigner.sign(input);

			// Assert:
			Assert.assertThat(dsaSigner.verify(input, signature1), IsEqual.equalTo(true));
		}
	}

	@Test
	public void verifyHasExpectedSpeed() {
		// Arrange:
		initCryptoEngine();
		final KeyPair keyPair = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(keyPair);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = dsaSigner.sign(input);

		// Warm up
		for (int i=0; i<1000; i++) {
			dsaSigner.verify(input, signature);
		}

		// Act:
		final long start = System.currentTimeMillis();
		for (int i=0; i<5000; i++) {
			dsaSigner.verify(input, signature);
		}
		final long stop = System.currentTimeMillis();

		// Assert (should be less than 500 micro seconds per verification on a decent computer):
		final long timeInMilliSeconds = stop - start;
		LOGGER.info(String.format("verify needs %d micro seconds.", timeInMilliSeconds / 5));

		Assert.assertThat(timeInMilliSeconds < 2500, IsEqual.equalTo(true));
	}

	@Override
	protected DsaSigner getDsaSigner(final KeyPair keyPair) {
		return new Ed25519DsaSigner(keyPair);
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
