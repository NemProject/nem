package org.nem.core.crypto;

/**
 * Interface for encryption and decryption of data.
 */
public interface BlockCipher {

	/**
	 * Encrypts an arbitrarily-sized message.
	 *
	 * @param input The message to encrypt.
	 * @return The encrypted message.
	 */
	byte[] encrypt(final byte[] input);

	/**
	 * Decrypts an arbitrarily-sized message.
	 *
	 * @param input The message to decrypt.
	 * @return The decrypted message or null if decryption failed.
	 */
	byte[] decrypt(final byte[] input);
}
