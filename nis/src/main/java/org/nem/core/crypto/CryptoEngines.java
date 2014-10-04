package org.nem.core.crypto;

/**
 * Static class that exposes crypto engines.
 */
public class CryptoEngines {

	public static CryptoEngine getDefaultEngine() {
		throw new RuntimeException("Not implemented yet.");
	}

	public interface CryptoEngine {

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
