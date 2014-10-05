package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.GroupElement;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Implementation of the key generator for Ed25519.
 */
public class Ed25519KeyGenerator implements KeyGenerator {
	private final SecureRandom random;

	public Ed25519KeyGenerator() {
		this.random = new SecureRandom();
	}

	@Override
	public KeyPair generateKeyPair() {
		final byte[] seed = new byte[32];
		random.nextBytes(seed);

		// seed is the private key.
		final PrivateKey privateKey = new PrivateKey(Utils.toBigInteger(seed));

		return new KeyPair(privateKey, derivePublicKey(privateKey));
	}

	@Override
	public PublicKey derivePublicKey(final PrivateKey privateKey) {
		// Hash the private key to improve randomness.
		final byte[] hash = Hashes.sha3(Utils.toByteArray(privateKey.getRaw()));

		// Only the lower 32 bytes are used for calculation of the public key.
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);

		// Clamp to resist small subgroup attacks.
		clamp(a);

		// a * base point is the public key.
		GroupElement pubKey = Ed25519Constants.basePoint.scalarMultiply(a);

		return new PublicKey(pubKey.toByteArray());
	}

	private void clamp(byte[] k) {
		k[31] &= 0x7F;
		k[31] |= 0x40;
		k[ 0] &= 0xF8;
	}
}
