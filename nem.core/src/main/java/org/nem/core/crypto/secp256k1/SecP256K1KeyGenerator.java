package org.nem.core.crypto.secp256k1;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;
import org.nem.core.crypto.*;

import java.security.SecureRandom;

/**
 * Implementation of the key generator for SECP256K1.
 */
public class SecP256K1KeyGenerator implements KeyGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	public KeyPair generateKeyPair() {
		final ECKeyPairGenerator generator = new ECKeyPairGenerator();
		final ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(SecP256K1Curve.secp256k1().getParams(), RANDOM);
		generator.init(keyGenParams);

		final AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
		final ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters)keyPair.getPrivate();
		final PrivateKey privateKey = new PrivateKey(privateKeyParams.getD());
		return new KeyPair(privateKey, CryptoEngines.secp256k1Engine());
	}

	@Override
	public PublicKey derivePublicKey(final PrivateKey privateKey) {
		final ECPoint point = SecP256K1Curve.secp256k1().getParams().getG().multiply(privateKey.getRaw());
		return new PublicKey(point.getEncoded(true));
	}
}
