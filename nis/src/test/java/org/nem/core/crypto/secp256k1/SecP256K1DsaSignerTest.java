package org.nem.core.crypto.secp256k1;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.utils.*;

import java.math.BigInteger;

public class SecP256K1DsaSignerTest extends DsaSignerTest {

	@Test
	public void signerProducesCorrectSignatureUsing256bitSha3() {
		// Arrange
		initCryptoEngine();
		final KeyPair keyPair = new KeyPair(new PrivateKey(BigInteger.valueOf(1L)));
		final DsaSigner dsaSigner = getDsaSigner(keyPair);

		// Act:
		final Signature signature = dsaSigner.sign(StringEncoder.getBytes("NEM"));

		// Assert:
		final String expectedSignature = "01485191de9fa79887300a2543e2ae5860c744863c380e9ccd2b0c62d768e61b68e3c1f8e8fe4206a4b598f512b5944a43cf8dac03fc871c2ed7d2b927643852";
		Assert.assertThat(HexEncoder.getString(signature.getBytes()), IsEqual.equalTo(expectedSignature));
	}

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
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
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final DsaSigner dsaSigner = getDsaSigner(kp);
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

	@Override
	protected DsaSigner getDsaSigner(final KeyPair keyPair) {
		return new SecP256K1DsaSigner(keyPair);
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}
