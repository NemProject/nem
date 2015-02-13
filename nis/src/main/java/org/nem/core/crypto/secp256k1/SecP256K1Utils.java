package org.nem.core.crypto.secp256k1;

import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;
import org.nem.core.crypto.*;

/**
 * Utility class for SECP256K1.
 */
public class SecP256K1Utils {

	/**
	 * Gets the EC private key parameters.
	 *
	 * @param privateKey The private key.
	 * @return The EC private key parameters.
	 */
	public static ECPrivateKeyParameters getPrivateKeyParameters(final PrivateKey privateKey) {
		return new ECPrivateKeyParameters(privateKey.getRaw(), SecP256K1Curve.secp256k1().getParams());
	}

	/**
	 * Gets the EC public key parameters.
	 *
	 * @param publicKey The public key.
	 * @return The EC public key parameters.
	 */
	public static ECPublicKeyParameters getPublicKeyParameters(final PublicKey publicKey) {
		final ECPoint point = SecP256K1Curve.secp256k1().getParams().getCurve().decodePoint(publicKey.getRaw());
		return new ECPublicKeyParameters(point, SecP256K1Curve.secp256k1().getParams());
	}
}
