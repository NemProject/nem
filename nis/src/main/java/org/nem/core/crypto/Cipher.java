package org.nem.core.crypto;

/**
 * Wraps IES encryption and decryption logic.
 */
public class Cipher {

	private final BlockCipher cipher;

	/**
	 * Creates a cipher around a sender KeyPair and recipient KeyPair.
	 *
	 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 */
	public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		this.cipher = CryptoEngines.getDefaultEngine().createBlockCipher(senderKeyPair, recipientKeyPair);
	}

	/**
	 * Encrypts an arbitrarily-sized message.
	 *
	 * @param input The message to encrypt.
	 * @return The encrypted message.
	 * @throws CryptoException if the encryption operation failed.
	 */
	public byte[] encrypt(final byte[] input) {
		return this.cipher.encrypt(input);
	}

	/**
	 * Decrypts an arbitrarily-sized message.
	 *
	 * @param input The message to decrypt.
	 * @return The decrypted message or null if decryption failed.
	 */
	public byte[] decrypt(final byte[] input) {
		return this.cipher.decrypt(input);
	}
}
