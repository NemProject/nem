package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

public class SecP256K1CryptoEngineTest extends CryptoEngineTest {

	@Override
	protected CryptoEngine getCryptoEngine() {
		return CryptoEngines.secp256k1Engine();
	}
}
