package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.*;

public class Ed25519KeyGeneratorTest extends KeyGeneratorTest {

	@Test
	public void derivedPublicKeyIsValidPointOnCurve() {
		// Arrange:
		final KeyGenerator generator = this.getKeyGenerator();
		for (int i = 0; i < 100; i++) {
			final KeyPair kp = generator.generateKeyPair();

			// Act:
			final PublicKey publicKey = generator.derivePublicKey(kp.getPrivateKey());

			// Assert (throws if not on the curve):
			new Ed25519EncodedGroupElement(publicKey.getRaw()).decode();
		}
	}

	@Test
	public void derivePublicKeyReturnsExpectedPublicKey() {
		// Arrange:
		final KeyGenerator generator = this.getKeyGenerator();
		for (int i = 0; i < 100; i++) {
			final KeyPair kp = generator.generateKeyPair();

			// Act:
			final PublicKey publicKey1 = generator.derivePublicKey(kp.getPrivateKey());
			final PublicKey publicKey2 = MathUtils.derivePublicKey(kp.getPrivateKey());

			// Assert:
			Assert.assertThat(publicKey1, IsEqual.equalTo(publicKey2));
		}
	}

	@Override
	protected CryptoEngine getCryptoEngine() {
		return CryptoEngines.ed25519Engine();
	}
}
