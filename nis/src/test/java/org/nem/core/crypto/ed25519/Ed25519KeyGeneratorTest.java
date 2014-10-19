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

	// The following "test" is for mining nice addresses
	/*@Test
	public void mineKeys() {
		// Arrange:
		initCryptoEngine();
		final KeyGenerator generator = getKeyGenerator();
		List<String> list = Arrays.asList( "ALICE", "TBOBB", "GIMRE", "THIES", "JAGUA", "MAKOT", "SANSAN", "JUSAN", "GOGOGO", "NIJUI", "HACHI", "RIGEL" );
		int i = 0;
		while (true) {
			final KeyPair keyPair = generator.generateKeyPair();
			final String address = Address.fromPublicKey(keyPair.getPublicKey()).getEncoded();
			for (String wantedAddress : list) {
				if (address.indexOf(wantedAddress) >= 0) {
					System.out.println(keyPair.getPrivateKey().toString() + " : " + keyPair.getPublicKey().toString() + " : " + address);
				}
			}
			i++;
			if (i % 20000 == 0) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}*/

	@Override
	protected CryptoEngine getCryptoEngine() {
		return CryptoEngines.ed25519Engine();
	}
}
