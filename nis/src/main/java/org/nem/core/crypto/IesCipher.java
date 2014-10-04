package org.nem.core.crypto;

/**
 * Interface for encryption and decryption of data.
 */
public interface IesCipher {

	/**
	 * Encrypts an arbitrarily-sized message.
	 *
	 * @param input The message to encrypt.
	 * @return The encrypted message.
	 * @throws CryptoException if the encryption operation failed.
	 */
	public byte[] encrypt(final byte[] input);

	/**
	 * Decrypts an arbitrarily-sized message.
	 *
	 * @param input The message to decrypt.
	 * @return The decrypted message or null if decryption failed.
	 */
	public byte[] decrypt(final byte[] input);
}
