package org.nem.core.crypto;

import org.nem.core.crypto.ed25519.Ed25519CryptoEngine;
import org.nem.core.crypto.secp256k1.SecP256K1CryptoEngine;

/**
 * Static class that exposes crypto engines.
 */
public class CryptoEngines {

	private static final CryptoEngine SECP256K1_ENGINE;
	private static final CryptoEngine ED25519_ENGINE;
	private static final CryptoEngine DEFAULT_ENGINE;

	static {
		SECP256K1_ENGINE = new SecP256K1CryptoEngine();
		ED25519_ENGINE = new Ed25519CryptoEngine();
		DEFAULT_ENGINE = ED25519_ENGINE;
	}

	/**
	 * Gets the default crypto engine.
	 *
	 * @return The default crypto engine.
	 */
	public static CryptoEngine defaultEngine() {
		return DEFAULT_ENGINE;
	}

	/**
	 * Gets the SECP256K1 crypto engine.
	 *
	 * @return The SECP256K1 crypto engine.
	 */
	public static CryptoEngine secp256k1Engine() {
		return SECP256K1_ENGINE;
	}

	/**
	 * Gets the ED25519 crypto engine.
	 *
	 * @return The ED25519 crypto engine.
	 */
	public static CryptoEngine ed25519Engine() {
		return ED25519_ENGINE;
	}
}
