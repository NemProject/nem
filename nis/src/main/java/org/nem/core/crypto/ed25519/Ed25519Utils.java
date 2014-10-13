package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.Hashes;
import org.nem.core.crypto.ed25519.arithmetic.Ed25519EncodedFieldElement;
import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utility methods for Ed25519.
 */
public class Ed25519Utils {

	// TODO 20141013 J-B: can we change the parameter to PrivateKey?

	/**
	 * Prepares a private key's raw value for scalar multiplication.
	 * The hashing is for achieving better randomness and the clamping prevents small subgroup attacks.
	 *
	 * @param value The private key's raw value.
	 * @return The prepared encoded field element.
	 */
	public static Ed25519EncodedFieldElement prepareForScalarMultiply(final BigInteger value) {
		final byte[] hash = Hashes.getSha3_512Instance().digest(ArrayUtils.toByteArray(value, 32));
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		a[31] &= 0x7F;
		a[31] |= 0x40;
		a[0] &= 0xF8;
		return new Ed25519EncodedFieldElement(a);
	}
}
