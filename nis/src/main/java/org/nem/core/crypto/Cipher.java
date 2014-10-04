package org.nem.core.crypto;

/**
 * Wraps IES encryption and decryption logic.
 */
public class Cipher {

	/*private final static IESParameters IES_PARAMETERS;

	static {
		final byte[] d = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final byte[] e = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
		IES_PARAMETERS = new IESParameters(d, e, 64);
	}

	private final IESEngine iesEncryptEngine;
	private final IESEngine iesDecryptEngine;*/
	private final IesCipher cipher;

	/**
	 * Creates a cipher around a sender KeyPair and recipient KeyPair.
	 *
	 * @param senderKeyPair The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 */
	public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		this.cipher = CryptoEngines.getDefaultEngine().createIesCipher(senderKeyPair, recipientKeyPair);
		/*
		if (senderKeyPair.hasPrivateKey()) {
			this.iesEncryptEngine = createIesEngine();
			this.iesEncryptEngine.init(
					true,
					senderKeyPair.getPrivateKeyParameters(),
					recipientKeyPair.getPublicKeyParameters(),
					IES_PARAMETERS);
		} else {
			this.iesEncryptEngine = null;
		}

		if (recipientKeyPair.hasPrivateKey()) {
			this.iesDecryptEngine = createIesEngine();
			this.iesDecryptEngine.init(
					false,
					recipientKeyPair.getPrivateKeyParameters(),
					senderKeyPair.getPublicKeyParameters(),
					IES_PARAMETERS);
		} else {
			this.iesDecryptEngine = null;
		}*/
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
		/*
		try {
			return this.iesEncryptEngine.processBlock(input, 0, input.length);
		} catch (final InvalidCipherTextException e) {
			throw new CryptoException(e);
		}*/
	}

	/**
	 * Decrypts an arbitrarily-sized message.
	 *
	 * @param input The message to decrypt.
	 * @return The decrypted message or null if decryption failed.
	 */
	public byte[] decrypt(final byte[] input) {
		return this.cipher.decrypt(input);
		/*
		try {
			return this.iesDecryptEngine.processBlock(input, 0, input.length);
		} catch (final InvalidCipherTextException e) {
			return null;
		}*/
	}

	/*private static IESEngine createIesEngine() {
		return new IESEngine(
				new ECDHBasicAgreement(),
				new KDF2BytesGenerator(new SHA1Digest()),
				new HMac(new SHA1Digest()));
	}*/
}