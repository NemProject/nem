package org.nem.core.crypto;

import org.nem.core.crypto.ed25519.Ed25519CryptoEngine;
import org.nem.core.crypto.secp256k1.SecP256K1CryptoEngine;
import org.nem.core.model.NemesisBlock;

/**
 * Static class that exposes crypto engines.
 */
public class CryptoEngines {

	private static final CryptoEngine secp256k1Engine;
	private static final CryptoEngine ed25519Engine;
	private static CryptoEngine defaultEngine = initDefaultEngine();

	static {
		secp256k1Engine = new SecP256K1CryptoEngine();
		ed25519Engine = new Ed25519CryptoEngine();
	}

	/**
	 * Initializes the default crypto engine (needed for unit tests).
	 *
	 * @return The default crypto engine.
	 */
	public static CryptoEngine initDefaultEngine() {
		defaultEngine = ed25519Engine;
		return defaultEngine;
	}

	/**
	 * Gets the default crypto engine.
	 *
	 * @return The default crypto engine.
	 */
	public static CryptoEngine getDefaultEngine() {
		if (null == defaultEngine) {
			initDefaultEngine();
		}

		return defaultEngine;
	}

	/**
	 * Sets the default crypto engine (needed for unit tests).
	 *
	 * @param engine The crypto engine.
	 */
	public static void setDefaultEngine(final CryptoEngine engine) {
		// TODO 20141012: this fixes the majority of the affected tests, but a few still fail
		// > because they use the default engine to check the validity of the nemesis address
		// the NemesisBlock depends on the "real" default engine,
		// so make sure it is initialized before changing the default engine
		//noinspection ResultOfMethodCallIgnored
		NemesisBlock.GENERATION_HASH.equals(Hash.ZERO);

		defaultEngine = engine;
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

	public interface CryptoEngine {

		/**
		 * Return The underlying curve.
		 *
		 * @return The curve.
		 */
		public Curve getCurve();

		/**
		 * Creates a DSA signer.
		 *
		 * @param keyPair The key pair.
		 * @return The DSA signer.
		 */
		public DsaSigner createDsaSigner(final KeyPair keyPair);

		/**
		 * Creates a key generator.
		 *
		 * @return The key generator.
		 */
		public KeyGenerator createKeyGenerator();

		/**
		 * Creates a block cipher.
		 *
		 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
		 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
		 * @return The IES cipher.
		 */
		public BlockCipher createBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair);

		/**
		 * Creates a key analyzer.
		 *
		 * @return The key analyzer.
		 */
		public KeyAnalyzer createKeyAnalyzer();
	}
}