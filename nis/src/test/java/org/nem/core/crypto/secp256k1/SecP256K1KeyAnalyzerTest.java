package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

public class SecP256K1KeyAnalyzerTest extends KeyAnalyzerTest {

	@Override
	protected KeyAnalyzer getKeyAnalyzer() {
		return new SecP256K1KeyAnalyzer();
	}

	@Override
	protected void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}
