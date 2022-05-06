package org.nem.core.crypto;

/**
 * Interface for generating keys.
 */
public interface KeyGenerator {

	/**
	 * Creates a random key pair.
	 *
	 * @return The key pair.
	 */
	KeyPair generateKeyPair();

	/**
	 * Derives a public key from a private key.
	 *
	 * @param privateKey the private key.
	 * @return The public key.
	 */
	PublicKey derivePublicKey(final PrivateKey privateKey);
}
