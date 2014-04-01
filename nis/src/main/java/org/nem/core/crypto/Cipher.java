package org.nem.core.crypto;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.macs.HMac;

/**
 * Wraps EC IES encryption and decryption logic.
 */
public class Cipher {

	private final static IESParameters IES_PARAMETERS;

	static {
		byte[] d = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		byte[] e = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
		IES_PARAMETERS = new IESParameters(d, e, 64);
	}

	private final IESEngine iesEncryptEngine;
	private final IESEngine iesDecryptEngine;

	/**
	 * Creates a cipher around a sender KeyPair and recipient KeyPair.
	 *
	 * @param senderKeyPair    The sender KeyPair. The sender's private key is required for encryption.
	 * @param recipientKeyPair The recipient KeyPair. The recipient's private key is required for decryption.
	 */
	public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
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
		}
	}

	/**
	 * Encrypts an arbitrarily-sized message.
	 *
	 * @param input The message to encrypt.
	 *
	 * @return The encrypted message.
	 * @throws CryptoException if the encryption operation failed.
	 */
	public byte[] encrypt(final byte[] input) {
		try {
			return this.iesEncryptEngine.processBlock(input, 0, input.length);
		} catch (InvalidCipherTextException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * Decrypts an arbitrarily-sized message.
	 *
	 * @param input The message to decrypt.
	 *
	 * @return The decrypted message or null if decryption failed.
	 */
	public byte[] decrypt(final byte[] input) {
		try {
			return this.iesDecryptEngine.processBlock(input, 0, input.length);
		} catch (InvalidCipherTextException e) {
			return null;
		}
	}

	private static IESEngine createIesEngine() {
		return new IESEngine(
				new ECDHBasicAgreement(),
				new KDF2BytesGenerator(new SHA1Digest()),
				new HMac(new SHA1Digest()));
	}
}