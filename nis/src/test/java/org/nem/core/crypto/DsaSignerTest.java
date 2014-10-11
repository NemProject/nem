package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public abstract class DsaSignerTest {

	@Test
	public void signedDataCanBeVerified() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);

		// Assert:
		Assert.assertThat(dsaSigner.verify(input, signature), IsEqual.equalTo(true));
	}

	@Test
	public void dataSignedWithKeyPairCannotBeVerifiedWithDifferentKeyPair() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();
		final KeyPair kp2 = new KeyPair();
		final DsaSigner dsaSigner1 = getDsaSigner(kp1);
		final DsaSigner dsaSigner2 = getDsaSigner(kp2);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature1 = dsaSigner1.sign(input);
		final Signature signature2 = dsaSigner2.sign(input);

		// Assert:
		Assert.assertThat(dsaSigner1.verify(input, signature1), IsEqual.equalTo(true));
		Assert.assertThat(dsaSigner1.verify(input, signature2), IsEqual.equalTo(false));
		Assert.assertThat(dsaSigner2.verify(input, signature1), IsEqual.equalTo(false));
		Assert.assertThat(dsaSigner2.verify(input, signature2), IsEqual.equalTo(true));
	}

	@Test
	public void signaturesReturnedBySignAreDeterministic() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final Signature signature1 = dsaSigner.sign(input);
		final Signature signature2 = dsaSigner.sign(input);

		// Assert:
		Assert.assertThat(signature1, IsEqual.equalTo(signature2));
	}

	@Test(expected = CryptoException.class)
	public void cannotSignPayloadWithoutPrivateKey() {
		// Arrange:
		final KeyPair kp = new KeyPair(Utils.generateRandomPublicKey());
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		dsaSigner.sign(input);
	}

	@Test
	public void isCanonicalReturnsTrueForCanonicalSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();

		// Act:
		final Signature signature = dsaSigner.sign(input);

		// Assert:
		Assert.assertThat(dsaSigner.isCanonicalSignature(signature), IsEqual.equalTo(true));
	}

	@Test
	public void verifyCallsIsCanonicalSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = Mockito.spy(getDsaSigner(kp));
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = new Signature(BigInteger.ONE, BigInteger.ONE);

		// Act:
		dsaSigner.verify(input, signature);

		// Assert:
		Mockito.verify(dsaSigner, Mockito.times(1)).isCanonicalSignature(signature);
	}

	protected abstract DsaSigner getDsaSigner(final KeyPair keyPair);

	protected abstract void initCryptoEngine();
}
