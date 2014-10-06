package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

public class SecP256K1KeyGeneratorTest extends KeyGeneratorTest {

	@Override
	protected KeyGenerator getKeyGenerator() {
		return new SecP256K1KeyGenerator();
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}
