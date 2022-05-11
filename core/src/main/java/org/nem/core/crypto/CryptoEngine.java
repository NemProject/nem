package org.nem.core.crypto;

/**
 * Represents a cryptographic engine that is a factory of crypto-providers.
 */
public interface CryptoEngine {

	/**
	 * Return The underlying curve.
	 *
	 * @return The curve.
	 */
	Curve getCurve();

	/**
	 * Creates a DSA signer.
	 *
	 * @param keyPair The key pair.
	 * @return The DSA signer.
	 */
	DsaSigner createDsaSigner(final KeyPair keyPair);

	/**
	 * Creates a key generator.
	 *
	 * @return The key generator.
	 */
	KeyGenerator createKeyGenerator();

	/**
	 * Creates a block cipher.
	 *
	 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 * @return The IES cipher.
	 */
	BlockCipher createBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair);

	/**
	 * Creates a key analyzer.
	 *
	 * @return The key analyzer.
	 */
	KeyAnalyzer createKeyAnalyzer();
}
