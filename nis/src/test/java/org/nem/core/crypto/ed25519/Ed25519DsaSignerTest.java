package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.MathUtils;

import java.math.BigInteger;

public class Ed25519DsaSignerTest extends DsaSignerTest {

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
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
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
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

	// Can rarely fail due to R not fitting into 32 bytes after adding the group order to it.
	@Test
	public void replacingRWithGroupOrderPlusRInSignatureRuinsSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final Signature signature2 = new Signature(
				CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().add(signature.getR()),
				signature.getS());

		// Assert:
		Assert.assertThat(dsaSigner.verify(input, signature2), IsEqual.equalTo(false));
	}

	@Test
	public void signReturnsExpectedSignature() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		for (int i = 0; i < 20; i++) {
			final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
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
		final KeyPair keyPair = new KeyPair();
		for (int i = 0; i < 20; i++) {
			final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
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
		final KeyPair keyPair = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = dsaSigner.sign(input);

		// Warm up
		for (int i = 0; i < 3000; i++) {
			dsaSigner.verify(input, signature);
		}

		// Act:
		final long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			dsaSigner.verify(input, signature);
		}
		final long stop = System.currentTimeMillis();

		// Assert (should be less than 500 micro seconds per verification on a decent computer):
		final long timeInMilliSeconds = stop - start;
		System.out.println(String.format("verify needs %d micro seconds.", timeInMilliSeconds / 10));
		Assert.assertTrue(
				String.format("verify needs %d micro seconds (expected less than 500 micro seconds).", timeInMilliSeconds / 10),
				timeInMilliSeconds < 5000);
	}

	@Override
	protected DsaSigner getDsaSigner(final KeyPair keyPair) {
		return new Ed25519DsaSigner(keyPair);
	}

	@Override
	@Before
	public void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
