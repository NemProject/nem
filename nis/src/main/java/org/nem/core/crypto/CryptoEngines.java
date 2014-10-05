package org.nem.core.crypto;

import org.nem.core.crypto.ed25519.Ed25519Engine;
import org.nem.core.crypto.secp256k1.SecP256K1Engine;

/**
 * Static class that exposes crypto engines.
 */
public class CryptoEngines {

	private static final CryptoEngine secp256k1Engine;
	private static final CryptoEngine ed25519Engine;

	static {
		secp256k1Engine = new SecP256K1Engine();
		ed25519Engine = new Ed25519Engine();
	}

	public static CryptoEngine getDefaultEngine() {
		return secp256k1Engine;
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
		 * Creates a IES cipher.
		 *
		 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
		 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
		 * @return The IES cipher.
		 */
		public IesCipher createIesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair);

		/**
		 * Creates a key analyzer.
		 *
		 * @return The key analyzer.
		 */
		public KeyAnalyzer createKeyAnalyzer();
	}
}
