package org.nem.core.crypto;

/**
 * Interface for generating keys.
 */
public interface KeyGenerator {

	/**
	 * Creates a random key pair.
	 */
	public KeyPair generateKeyPair();

	/**
	 * Derives a public key from a private key.
	 *
	 * @param privateKey the private key.
	 * @return The public key.
	 */
	public PublicKey derivePublicKey(final PrivateKey privateKey);
}
