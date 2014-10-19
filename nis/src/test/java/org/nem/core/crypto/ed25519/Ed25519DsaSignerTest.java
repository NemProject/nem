package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.MathUtils;

import java.math.BigInteger;

public class Ed25519DsaSignerTest extends DsaSignerTest {

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair kp = KeyPair.random(engine);
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = engine.getCurve().getGroupOrder().add(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(nonCanonicalSignature), IsEqual.equalTo(false));
	}

	@Test
	public void makeCanonicalMakesNonCanonicalSignatureCanonical() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair kp = KeyPair.random(engine);
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);
		final BigInteger nonCanonicalS = engine.getCurve().getGroupOrder().add(signature.getS());
		final Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);
		Assert.assertThat(dsaSigner.isCanonicalSignature(nonCanonicalSignature), IsEqual.equalTo(false));
		final Signature canonicalSignature = dsaSigner.makeSignatureCanonical(nonCanonicalSignature);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(canonicalSignature), IsEqual.equalTo(true));
	}

	@Test
	public void replacingRWithGroupOrderPlusRInSignatureRuinsSignature() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final BigInteger groupOrder = engine.getCurve().getGroupOrder();
		final KeyPair kp = KeyPair.random(engine);
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		Signature signature;
		byte[] input;
		while (true) {
			input = org.nem.core.test.Utils.generateRandomBytes();
			signature = dsaSigner.sign(input);
			if (signature.getR().add(groupOrder).compareTo(BigInteger.ONE.shiftLeft(256)) < 0) {
				break;
			}
		}

		// Act:
		final Signature signature2 = new Signature(groupOrder.add(signature.getR()), signature.getS());

		// Assert:
		Assert.assertThat(dsaSigner.verify(input, signature2), IsEqual.equalTo(false));
	}

	@Test
	public void signReturnsExpectedSignature() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair keyPair = KeyPair.random(engine);
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
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair keyPair = KeyPair.random(engine);
		for (int i = 0; i < 20; i++) {
			final DsaSigner dsaSigner = this.getDsaSigner(keyPair);
			final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

			// Act:
			final Signature signature1 = dsaSigner.sign(input);

			// Assert:
			Assert.assertThat(dsaSigner.verify(input, signature1), IsEqual.equalTo(true));
		}
	}

	@Test(expected = CryptoException.class)
	public void signThrowsIfGeneratedSignatureIsNotCanonical() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair keyPair = KeyPair.random(engine);
		final Ed25519DsaSigner dsaSigner = Mockito.mock(Ed25519DsaSigner.class);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		Mockito.when(dsaSigner.getKeyPair()).thenReturn(keyPair);
		Mockito.when(dsaSigner.sign(input)).thenCallRealMethod();
		Mockito.when(dsaSigner.isCanonicalSignature(Mockito.any())).thenReturn(false);

		// Act:
		dsaSigner.sign(input);
	}

	@Test
	public void verifyReturnsFalseIfPublicKeyIsZeroArray() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair kp = KeyPair.random(engine);
		final DsaSigner dsaSigner = this.getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = dsaSigner.sign(input);
		final Ed25519DsaSigner dsaSignerWithZeroArrayPublicKey = Mockito.mock(Ed25519DsaSigner.class);
		Mockito.when(dsaSignerWithZeroArrayPublicKey.getKeyPair())
				.thenReturn(new KeyPair(kp.getPrivateKey(), new PublicKey(new byte[32]), engine));
		Mockito.when(dsaSignerWithZeroArrayPublicKey.verify(input, signature)).thenCallRealMethod();
		Mockito.when(dsaSignerWithZeroArrayPublicKey.isCanonicalSignature(signature)).thenReturn(true);

		// Act:
		final boolean result = dsaSignerWithZeroArrayPublicKey.verify(input, signature);

		// Assert (getKeyPair() would be called more than once if it got beyond the second check):
		Assert.assertThat(result, IsEqual.equalTo(false));
		Mockito.verify(dsaSignerWithZeroArrayPublicKey, Mockito.times(1)).isCanonicalSignature(signature);
		Mockito.verify(dsaSignerWithZeroArrayPublicKey, Mockito.times(1)).getKeyPair();
	}

	@Test
	public void verifyHasExpectedSpeed() {
		// Arrange:
		final CryptoEngines.CryptoEngine engine = this.getCryptoEngine();
		final KeyPair keyPair = KeyPair.random(engine);
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
	protected CryptoEngines.CryptoEngine getCryptoEngine() {
		return CryptoEngines.ed25519Engine();
	}
}
