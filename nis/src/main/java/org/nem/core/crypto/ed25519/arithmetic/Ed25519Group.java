package org.nem.core.crypto.ed25519.arithmetic;

import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

/**
 * Represents the underlying group for Ed25519.
 */
public class Ed25519Group {

	// Group related constants.
	public static Ed25519GroupElement BASE_POINT = getBasePoint();
	public static Ed25519GroupElement ZERO = Ed25519GroupElement.p3(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
	public static Ed25519GroupElement ZERO_P2 = Ed25519GroupElement.p2(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE);
	public static Ed25519GroupElement ZERO_PRECOMP = Ed25519GroupElement.precomp(Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
	public static final BigInteger GROUP_ORDER = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));


	public static Ed25519GroupElement getBasePoint() {
		BASE_POINT = fromEncoded(HexEncoder.getBytes("5866666666666666666666666666666666666666666666666666666666666666"));
		BASE_POINT.precomputeForScalarMultiplication();
		BASE_POINT.precomputeForDoubleScalarMultiplication();
		return BASE_POINT;
	}

	public static Ed25519GroupElement fromEncoded(final byte[] encoded) {
		return new Ed25519GroupElement(encoded);
	}
}
