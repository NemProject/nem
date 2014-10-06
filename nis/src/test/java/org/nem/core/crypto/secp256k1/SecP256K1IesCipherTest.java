package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

public class SecP256K1IesCipherTest extends IesCipherTest {

	@Override
	protected IesCipher getIesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new SecP256K1IesCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}
