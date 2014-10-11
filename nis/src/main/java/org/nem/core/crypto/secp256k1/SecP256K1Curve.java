package org.nem.core.crypto.secp256k1;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.nem.core.crypto.Curve;

import java.math.BigInteger;

/**
 * Class that wraps the elliptic curve SECP256K1.
 */
public class SecP256K1Curve implements Curve {
	private static final SecP256K1Curve SECP256K1;
	private final ECDomainParameters params;
	private final BigInteger halfGroupOrder;

	static {
		final X9ECParameters params = SECNamedCurves.getByName("secp256k1");
		final ECDomainParameters ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
		SECP256K1 = new SecP256K1Curve(ecParams, ecParams.getN().shiftRight(1));
	}

	private SecP256K1Curve(final ECDomainParameters params, final BigInteger halfGroupOrder) {
		this.params = params;
		this.halfGroupOrder = halfGroupOrder;
	}

	@Override
	public String getName() {
		return "secp256k1";
	}

	@Override
	public BigInteger getGroupOrder() {
		return this.params.getN();
	}

	@Override
	public BigInteger getHalfGroupOrder() {
		return this.halfGroupOrder;
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
	 * Gets the SECP256K1 instance.
	 *
	 * @return The SECP256K1 instance.
	 */
	public static SecP256K1Curve secp256k1() {
		return SECP256K1;
	}
}
