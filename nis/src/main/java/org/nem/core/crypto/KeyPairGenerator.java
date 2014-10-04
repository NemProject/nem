package org.nem.core.crypto;

/**
 * Interface for generating key pairs.
 */
public interface KeyPairGenerator {

	/**
	 * Creates a random key pair.
	 */
	public KeyPair generateKeyPair();
}
