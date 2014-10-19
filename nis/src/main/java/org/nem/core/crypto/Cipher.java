package org.nem.core.crypto;

/**
 * Wraps IES encryption and decryption logic.
 */
public class Cipher implements BlockCipher {
	private final BlockCipher cipher;

	/**
	 * Creates a cipher around a sender KeyPair and recipient KeyPair.
	 *
	 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 */
	public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		this(senderKeyPair, recipientKeyPair, CryptoEngines.defaultEngine());
	}

	/**
	 * Creates a cipher around a sender KeyPair and recipient KeyPair.
	 *
	 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 * @param engine The crypto engine.
	 */
	public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair, final CryptoEngine engine) {
		this(engine.createBlockCipher(senderKeyPair, recipientKeyPair));
	}

	/**
	 * Creates a cipher around a cipher.
	 *
	 * @param cipher The cipher.
	 */
	public Cipher(final BlockCipher cipher) {
		this.cipher = cipher;
	}

	@Override
	public byte[] encrypt(final byte[] input) {
		return this.cipher.encrypt(input);
	}

	@Override
	public byte[] decrypt(final byte[] input) {
		return this.cipher.decrypt(input);
	}
}
