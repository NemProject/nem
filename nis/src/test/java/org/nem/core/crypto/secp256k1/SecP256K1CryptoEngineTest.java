package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

public class SecP256K1CryptoEngineTest extends CryptoEngineTest {

	// TODO 20141010 J-B: you might want to validate the curve returned is valid too (the base class validates 4/5 properties)
	// TODO 20141011 BR -> J: Added test in base class.

	@Override
	protected CryptoEngines.CryptoEngine getCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
		return CryptoEngines.getDefaultEngine();
	}
}
