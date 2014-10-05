package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.arithmetic.GroupElement;
import org.nem.core.crypto.ed25519.spec.*;

import java.math.BigInteger;

/**
 * Constants for ed25519.
 */
public class Ed25519Constants {

	private static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName("ed25519-sha-512");
	public static final Curve curve = ed25519.getCurve();
	public static GroupElement basePoint = getBasePoint();
	public static final BigInteger groupOrder = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));


	public static GroupElement getBasePoint() {
		basePoint = new GroupElement(ed25519.getCurve(), Utils.hexToBytes("5866666666666666666666666666666666666666666666666666666666666666"));
		basePoint.precompute(true);
		return basePoint;
	}
}
