package org.nem.core.crypto.ed25519.arithmetic;

import org.nem.core.utils.HexEncoder;
/**
 * Represents the underlying finite field for Ed25519.
 * The field has p = 2^255 - 19 elements.
 */
public class Ed25519Field {

	private final byte[] P = HexEncoder.getBytes("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f");
	public final Ed25519FieldElement ZERO = getFieldElement(0);

	private static Ed25519FieldElement getFieldElement(final int value) {
		final int[] f = new int[10];
		f[0] = value;
		return new Ed25519FieldElement(f);
	}
}
