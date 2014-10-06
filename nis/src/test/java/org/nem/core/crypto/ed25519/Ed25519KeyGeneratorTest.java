package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

public class Ed25519KeyGeneratorTest extends KeyGeneratorTest {

	@Override
	protected KeyGenerator getKeyGenerator() {
		return new Ed25519KeyGenerator();
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
