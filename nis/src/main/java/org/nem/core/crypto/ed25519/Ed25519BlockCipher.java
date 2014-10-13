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
		final byte[] salt = new byte[recipientKeyPair.getPublicKey().getRaw().length];
		this.random.nextBytes(salt);

		// Derive shared key.
		final byte[] sharedKey = this.getSharedKey(this.senderKeyPair.getPrivateKey(), this.recipientKeyPair.getPublicKey(), salt);

		// Setup IV.
		final byte[] ivData = new byte[16];
		this.random.nextBytes(ivData);

		// Setup block cipher.
		final BufferedBlockCipher cipher = this.setupBlockCipher(sharedKey, ivData, true);

		// Encode.
		final int[] length = new int[1];
		final byte[] buf = transform(cipher, input, length);
		if (null == buf) {
			return null;
		}

		final byte[] result = new byte[salt.length + ivData.length + length[0]];
		System.arraycopy(salt, 0, result, 0, salt.length);
		System.arraycopy(ivData, 0, result, salt.length, ivData.length);
		System.arraycopy(buf, 0, result, salt.length + ivData.length, length[0]);
		return result;
	}

	@Override
	public byte[] decrypt(final byte[] input) {
		// TODO 20141011 J-B: consider adding a test that decryption fails if input is too small
		// TODO 20141012 BR -> J: done.
		if (input.length < 64) {
			return null;
		}

		final int keyLength = this.senderKeyPair.getPublicKey().getRaw().length;
		final byte[] salt = Arrays.copyOfRange(input, 0, keyLength);
		final byte[] ivData = Arrays.copyOfRange(input, keyLength, 48);
		final byte[] encData = Arrays.copyOfRange(input, 48, input.length);

		// Derive shared key.
		final byte[] sharedKey = this.getSharedKey(this.recipientKeyPair.getPrivateKey(), this.senderKeyPair.getPublicKey(), salt);

		// Setup block cipher.
		final BufferedBlockCipher cipher = this.setupBlockCipher(sharedKey, ivData, false);

		// Decode.
		// TODO 20141011 J-B: consider refactoring this block (same as in encode)
		// TODO 20141012 BR -> J: Doesn't look much better imo ^^
		final int[] length = new int[1];
		final byte[] buf = transform(cipher, encData, length);
		if (null == buf) {
			return null;
		}

		// Remove padding
		final byte[] out = new byte[length[0]];
		System.arraycopy(buf, 0, out, 0, length[0]);
		return out;
	}

	private byte[] transform(final BufferedBlockCipher cipher, final byte[] data, final int[] length) {
		final byte[] buf = new byte[cipher.getOutputSize(data.length)];
		length[0] = cipher.processBytes(data, 0, data.length, buf, 0);
		try {
			length[0] += cipher.doFinal(buf, length[0]);
		} catch (final InvalidCipherTextException e) {
			return null;
		}

		return buf;
	}

	private BufferedBlockCipher setupBlockCipher(final byte[] sharedKey, final byte[] ivData, final boolean forEncryption) {
		// Setup cipher parameters with key and IV.
		final KeyParameter keyParam = new KeyParameter(sharedKey);
		final CipherParameters params = new ParametersWithIV(keyParam, ivData);

		// Setup AES cipher in CBC mode with PKCS7 padding.
		final BlockCipherPadding padding = new PKCS7Padding();
		final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
		cipher.reset();
		cipher.init(forEncryption, params);
		return cipher;
	}

	private byte[] getSharedKey(final PrivateKey privateKey, final PublicKey publicKey, final byte[] salt) {
		final Ed25519GroupElement senderA = new Ed25519EncodedGroupElement(publicKey.getRaw()).decode();
		senderA.precomputeForScalarMultiplication();
		final byte[] sharedKey = senderA.scalarMultiply(Ed25519Utils.prepareForScalarMultiply(privateKey.getRaw())).encode().getRaw();
		// TODO 20141011 J-B: consider using a constant for the key / salt length
		// TODO 20141012 BR -> J: salt length is now coupled to the public key length.
		for (int i = 0; i < publicKey.getRaw().length; i++) {
			sharedKey[i] ^= salt[i];
		}

		return Hashes.sha3(sharedKey);
	}
}
