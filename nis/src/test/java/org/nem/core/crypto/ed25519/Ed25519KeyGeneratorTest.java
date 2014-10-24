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
		final KeyGenerator generator = getKeyGenerator();
		List<String> list = new LinkedList(
			Arrays.asList(
					"TALICE2", "TALICE3", "TALICE4", "TALICE5",
					"TALICE6", "TALICE7", "TALICE8", "TALICE9")
		);
		int i = 0;
		while (true) {
			final KeyPair keyPair = generator.generateKeyPair();
			final String address = Address.fromPublicKey(keyPair.getPublicKey()).getEncoded();
			Iterator<String> iterator = list.listIterator();
			while (iterator.hasNext()) {
				final String wantedAddress = iterator.next();
				if (address.startsWith(wantedAddress)) {
					System.out.println(keyPair.getPrivateKey().toString() + " : " + keyPair.getPublicKey().toString() + " : " + address);
					iterator.remove();
					break;
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
