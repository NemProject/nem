package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

public class Ed25519BlockCipherTest extends BlockCipherTest {

	@Override
	protected BlockCipher getBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new Ed25519BlockCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
