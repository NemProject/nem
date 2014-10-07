package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.GroupElement;

import java.security.SecureRandom;

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
		this.random.nextBytes(seed);

		// seed is the private key.
		final PrivateKey privateKey = new PrivateKey(Utils.toBigInteger(seed));

		return new KeyPair(privateKey, derivePublicKey(privateKey));
	}

	@Override
	public PublicKey derivePublicKey(final PrivateKey privateKey) {
		final byte[] a = privateKey.prepareForScalarMultiply();

		// a * base point is the public key.
		final GroupElement pubKey = Ed25519Constants.basePoint.scalarMultiply(a);

		return new PublicKey(
				pubKey.toByteArray(),
				pubKey.getX().getRaw(),
				pubKey.getY().getRaw(),
				pubKey.getZ().getRaw(),
				pubKey.getT().getRaw());
	}
}
