package org.nem.core.crypto.ed25519;

import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.*;
import org.bouncycastle.crypto.params.*;
import org.nem.core.crypto.BlockCipher;
import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.*;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Implementation of the block cipher for Ed25519.
 */
public class Ed25519BlockCipher implements BlockCipher {

	private final KeyPair senderKeyPair;
	private final KeyPair recipientKeyPair;
	private final SecureRandom random;

	public Ed25519BlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		this.senderKeyPair = senderKeyPair;
		this.recipientKeyPair = recipientKeyPair;
		this.random = new SecureRandom();
	}

	@Override
	public byte[] encrypt(final byte[] input) {
		// Setup salt.
		final byte[] salt = new byte[32];
		this.random.nextBytes(salt);

		// Derive shared key.
		final byte[] sharedKey = getSharedKey(this.senderKeyPair.getPrivateKey(), this.recipientKeyPair.getPublicKey(), salt);

		// Setup IV.
		final byte[] ivData = new byte[16];
		this.random.nextBytes(ivData);

		// Setup block cipher.
		BufferedBlockCipher cipher = setupBlockCipher(sharedKey, ivData, true);

		// Encode.
		byte[] buf = new byte[cipher.getOutputSize(input.length)];
		int len = cipher.processBytes(input, 0, input.length, buf, 0);
		try {
			len += cipher.doFinal(buf, len);
		} catch (final InvalidCipherTextException e) {
			return null;
		}

		byte[] result = new byte[salt.length + ivData.length + len];
		System.arraycopy(salt, 0, result, 0, salt.length);
		System.arraycopy(ivData, 0, result, salt.length, ivData.length);
		System.arraycopy(buf, 0, result, salt.length + ivData.length, len);

		return result;
	}

	@Override
	public byte[] decrypt(final byte[] input) {
		if (input.length < 64) {
			return null;
		}

		final byte[] salt = Arrays.copyOfRange(input, 0, 32);
		final byte[] ivData = Arrays.copyOfRange(input, 32, 48);
		final byte[] encData = Arrays.copyOfRange(input, 48, input.length);

		// Derive shared key.
		final byte[] sharedKey = getSharedKey(this.recipientKeyPair.getPrivateKey(), this.senderKeyPair.getPublicKey(), salt);

		// Setup block cipher.
		BufferedBlockCipher cipher = setupBlockCipher(sharedKey, ivData, false);

		// Decode.
		byte[] buf = new byte[cipher.getOutputSize(encData.length)];
		int len = cipher.processBytes(encData, 0, encData.length, buf, 0);
		try {
			len += cipher.doFinal(buf, len);
		} catch (final InvalidCipherTextException e) {
			return null;
		}

		// Remove padding
		byte[] out = new byte[len];
		System.arraycopy(buf, 0, out, 0, len);

		return out;
	}

	private BufferedBlockCipher setupBlockCipher(final byte[] sharedKey, final byte[] ivData, final boolean forEncryption) {
		// Setup cipher parameters with key and IV.
		KeyParameter keyParam = new KeyParameter(sharedKey);
		CipherParameters params = new ParametersWithIV(keyParam, ivData);

		// Setup AES cipher in CBC mode with PKCS7 padding.
		BlockCipherPadding padding = new PKCS7Padding();
		BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
		cipher.reset();
		cipher.init(forEncryption, params);

		return cipher;
	}

	private byte[] getSharedKey(final PrivateKey privateKey, final PublicKey publicKey, final byte[] salt) {
		final Ed25519GroupElement senderA = new Ed25519GroupElement(new Ed25519EncodedFieldElement(publicKey.getRaw()));
		senderA.precomputeForScalarMultiplication();
		final byte[] sharedKey = senderA.scalarMultiply(privateKey.prepareForScalarMultiply()).toByteArray();
		for (int i = 0; i < 32; i++) {
			sharedKey[i] ^= salt[i];
		}
		return Hashes.sha3(sharedKey);
	}
}
