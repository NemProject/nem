package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

public class Ed25519IesCipherTest extends IesCipherTest {

	@Override
	protected IesCipher getIesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new Ed25519IesCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
