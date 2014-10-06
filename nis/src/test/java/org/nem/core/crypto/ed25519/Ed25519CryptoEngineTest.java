package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

public class Ed25519CryptoEngineTest extends CryptoEngineTest {

	@Override
	protected CryptoEngines.CryptoEngine getCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
		return CryptoEngines.getDefaultEngine();
	}
}
