package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.Ed25519EncodedFieldElement;
import org.nem.core.utils.ArrayUtils;

import java.util.Arrays;

/**
 * Utility methods for Ed25519.
 */
public class Ed25519Utils {

	/**
	 * Prepares a private key's raw value for scalar multiplication.
	 * The hashing is for achieving better randomness and the clamping prevents small subgroup attacks.
	 *
	 * @param key The private key.
	 * @return The prepared encoded field element.
	 */
	public static Ed25519EncodedFieldElement prepareForScalarMultiply(final PrivateKey key) {
		final byte[] hash = Hashes.sha3_512(ArrayUtils.toByteArray(key.getRaw(), 32));
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		a[31] &= 0x7F;
		a[31] |= 0x40;
		a[0] &= 0xF8;
		return new Ed25519EncodedFieldElement(a);
	}
}
