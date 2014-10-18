package org.nem.core.crypto.ed25519;

import org.junit.Before;
import org.nem.core.crypto.*;

public class Ed25519KeyAnalyzerTest extends KeyAnalyzerTest {

	@Override
	protected CryptoEngines.CryptoEngine getCryptoEngine() {
		return CryptoEngines.ed25519Engine();
	}
}
