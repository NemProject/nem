package org.nem.core.crypto;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

import java.math.BigInteger;

/**
 * Static class that exposes EC curves.
 */
public class Curves {

	private static final Curve SECP256K1;

	static {
		final X9ECParameters params = SECNamedCurves.getByName("secp256k1");
		final ECDomainParameters ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
		SECP256K1 = new Curve(ecParams, ecParams.getN().shiftRight(1));
	}

	/**
	 * Describes a curve.
	 */
	public static class Curve {
		private final ECDomainParameters params;
		private final BigInteger halfCurveOrder;

		private Curve(final ECDomainParameters params, final BigInteger halfCurveOrder) {
			this.params = params;
			this.halfCurveOrder = halfCurveOrder;
		}

		/**
		 * Gets the curve parameters.
		 *
		 * @return The curve parameters.
		 */
		public ECDomainParameters getParams() {
			return this.params;
		}

		/**
		 * Gets the curve half order.
		 *
		 * @return The curve half order.
		 */
		public BigInteger getHalfCurveOrder() {
			return this.halfCurveOrder;
		}
	}

	/**
	 * Returns information about the secp256k1 curve.
	 *
	 * @return Information about the secp256k1 curve.
	 */
	public static Curve secp256k1() {
		return SECP256K1;
	}
}
