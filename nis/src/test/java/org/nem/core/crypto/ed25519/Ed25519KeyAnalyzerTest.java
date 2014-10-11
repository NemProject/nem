package org.nem.core.crypto.ed25519;

import org.junit.Before;
import org.nem.core.crypto.*;

public class Ed25519KeyAnalyzerTest extends KeyAnalyzerTest {

	@Override
	protected KeyAnalyzer getKeyAnalyzer() {
		return new Ed25519KeyAnalyzer();
	}

	@Override
	@Before
	public void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.ed25519Engine());
	}
}
