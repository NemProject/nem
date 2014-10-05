package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

/**
 * Class that wraps the SECP256K1 specific implementation.
 */
public class SecP256K1Engine implements CryptoEngines.CryptoEngine {

	@Override
	public Curve getCurve() {
		return SecP256K1Curve.secp256k1();
	}

	@Override
	public DsaSigner createDsaSigner(final KeyPair keyPair) {
		return new SecP256K1DsaSigner(keyPair);
	}

	@Override
	public KeyGenerator createKeyGenerator() {
		return new SecP256K1keyGenerator();
	}

	@Override
	public IesCipher createIesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new SecP256K1IesCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	public KeyAnalyzer createKeyAnalyzer() {
		return new SecP256K1KeyAnalyzer();
	}
}
