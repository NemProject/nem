package org.nem.core.crypto;

import org.nem.core.crypto.ed25519.Ed25519CryptoEngine;
import org.nem.core.crypto.secp256k1.SecP256K1CryptoEngine;

/**
 * Static class that exposes crypto engines.
 */
public class CryptoEngines {

	private static final CryptoEngine secp256k1Engine;
	private static final CryptoEngine ed25519Engine;
	private static final CryptoEngine defaultEngine;

	static {
		secp256k1Engine = new SecP256K1CryptoEngine();
		ed25519Engine = new Ed25519CryptoEngine();
		defaultEngine = ed25519Engine;
	}

	/**
	 * Gets the default crypto engine.
	 *
	 * @return The default crypto engine.
	 */
	public static CryptoEngine defaultEngine() {
		return defaultEngine;
	}

	/**
	 * Gets the SECP256K1 crypto engine.
	 *
	 * @return The SECP256K1 crypto engine.
	 */
	public static CryptoEngine secp256k1Engine() {
		return secp256k1Engine;
	}

	/**
	 * Gets the ED25519 crypto engine.
	 *
	 * @return The ED25519 crypto engine.
	 */
	public static CryptoEngine ed25519Engine() {
		return ed25519Engine;
	}
}
