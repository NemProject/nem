package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.crypto.*;

public class Ed25519BlockCipherTest extends BlockCipherTest {

	@Test
	public void decryptReturnsNullIfInputIsTooSmallInLength() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final KeyPair kp = KeyPair.random(engine);
		final BlockCipher blockCipher = this.getBlockCipher(kp, kp);

		// Act:
		final byte[] decryptedBytes = blockCipher.decrypt(new byte[63]);

		// Assert:
		Assert.assertThat(decryptedBytes, IsNull.nullValue());
	}

	@Override
	protected BlockCipher getBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new Ed25519BlockCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	protected CryptoEngine getCryptoEngine() {
		return CryptoEngines.ed25519Engine();
	}
}
