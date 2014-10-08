package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.arithmetic.*;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

/**
 * Constants for ed25519.
 */
public class Ed25519Constants {

	private static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName("ed25519-sha-512");
	public static final Curve curve = ed25519.getCurve();

	// Group related constants.
	public static Ed25519GroupElement basePoint = getBasePoint();
	public static Ed25519GroupElement ZERO = Ed25519GroupElement.p3(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
	public static Ed25519GroupElement ZERO_P2 = Ed25519GroupElement.p2(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE);
	public static Ed25519GroupElement ZERO_PRECOMP = Ed25519GroupElement.precomp(Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
	public static final BigInteger groupOrder = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));


	public static Ed25519GroupElement getBasePoint() {
		basePoint = new Ed25519GroupElement(ed25519.getCurve(), HexEncoder.getBytes("5866666666666666666666666666666666666666666666666666666666666666"));
		basePoint.precompute(true);
		return basePoint;
	}
}
