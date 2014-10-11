package org.nem.core.crypto.secp256k1;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.*;

import java.math.BigInteger;

public class SecP256K1DsaSignerTest extends DsaSignerTest {

	@Test
	public void signerProducesCorrectSignatureUsing256bitSha3() {
		// Arrange
		final KeyPair keyPair = new KeyPair(new PrivateKey(BigInteger.valueOf(1L)));
		final DsaSigner dsaSigner = this.getDsaSigner(keyPair);

		// Act:
		final Signature signature = dsaSigner.sign(StringEncoder.getBytes("NEM"));

		// Assert:
		final String expectedSignature = "01485191de9fa79887300a2543e2ae5860c744863c380e9ccd2b0c62d768e61b68e3c1f8e8fe4206a4b598f512b5944a43cf8dac03fc871c2ed7d2b927643852";
		Assert.assertThat(HexEncoder.getString(signature.getBytes()), IsEqual.equalTo(expectedSignature));
	}

	// TODO 20141010 J-B: i think verifyReturnsFalseForNonCanonicalSignature is still valid
	// TODO 20141011 BR -> J: added the test though I don't think we need it as we have
	// TODO                   1) DsaSignerTest.verifyCallsIsCanonicalSignature to ensure IsCanonicalSignature is called and
	// TODO                   2) isCanonicalReturnsFalseForNonCanonicalSignature to ensure IsCanonicalSignature returns false for non-canonical signatures.

	@Test
	public void verifyReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

		// Assert:
		Assert.assertThat(dsaSigner.verify(input, nonCanonicalSignature), IsEqual.equalTo(false));
	}

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getS());
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
		final BigInteger nonCanonicalS = CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);
		Assert.assertThat(dsaSigner.isCanonicalSignature(nonCanonicalSignature), IsEqual.equalTo(false));
		final Signature canonicalSignature = dsaSigner.makeSignatureCanonical(nonCanonicalSignature);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(canonicalSignature), IsEqual.equalTo(true));
	}

	@Test
	public void replacingRWithGroupOrderMinusRInSignatureRuinsSignature() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final Signature signature2 = new Signature(
				CryptoEngines.getDefaultEngine().getCurve().getGroupOrder().subtract(signature.getR()),
				signature.getS());

		// Assert:
		Assert.assertThat(dsaSigner.verify(input, signature2), IsEqual.equalTo(false));
	}

	// TODO 201401010: i wonder if we should move the speed tests out of the unit tests?
	// > don't do this now as i'm not sure it's worth it :), but these might be too slow to be in the regular batch of tests
	@Test
	public void verifyHasExpectedSpeed() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = dsaSigner.sign(input);

		// Warm up
		for (int i = 0; i < 500; i++) {
			dsaSigner.verify(input, signature);
		}

		// Act:
		final long start = System.currentTimeMillis();
		for (int i = 0; i < 500; i++) {
			dsaSigner.verify(input, signature);
		}
		final long stop = System.currentTimeMillis();

		// Assert (should be less than 2500 micro seconds per verification on a decent computer):
		final long timeInMilliSeconds = stop - start;
		System.out.println(String.format("verify needs %d micro seconds.", timeInMilliSeconds * 2));
		Assert.assertTrue(
				String.format("verify needs %d micro seconds (expected less than 500 micro seconds).", timeInMilliSeconds * 2),
				timeInMilliSeconds < 2500);
	}

	@Override
	protected DsaSigner getDsaSigner(final KeyPair keyPair) {
		return new SecP256K1DsaSigner(keyPair);
	}

	@Override
	@Before
	public void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}
