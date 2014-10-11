package org.nem.core.crypto.secp256k1;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;
import org.nem.core.crypto.*;

/**
 * Implementation of the block cipher for SECP256K1.
 */
public class SecP256K1BlockCipher implements BlockCipher {

	private final static IESParameters IES_PARAMETERS;

	static {
		final byte[] d = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final byte[] e = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
		IES_PARAMETERS = new IESParameters(d, e, 64);
	}

	private final IESEngine iesEncryptEngine;
	private final IESEngine iesDecryptEngine;

	public SecP256K1BlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		if (senderKeyPair.hasPrivateKey()) {
			this.iesEncryptEngine = createIesEngine();
			this.iesEncryptEngine.init(
					true,
					getPrivateKeyParameters(senderKeyPair.getPrivateKey()),
					getPublicKeyParameters(recipientKeyPair.getPublicKey()),
					IES_PARAMETERS);
		} else {
			this.iesEncryptEngine = null;
		}

		if (recipientKeyPair.hasPrivateKey()) {
			this.iesDecryptEngine = createIesEngine();
			this.iesDecryptEngine.init(
					false,
					getPrivateKeyParameters(recipientKeyPair.getPrivateKey()),
					getPublicKeyParameters(senderKeyPair.getPublicKey()),
					IES_PARAMETERS);
		} else {
			this.iesDecryptEngine = null;
		}
	}

	@Override
	public byte[] encrypt(final byte[] input) {
		try {
			return this.iesEncryptEngine.processBlock(input, 0, input.length);
		} catch (final InvalidCipherTextException e) {
			throw new CryptoException(e);
		}
	}

	@Override
	public byte[] decrypt(final byte[] input) {
		try {
			return this.iesDecryptEngine.processBlock(input, 0, input.length);
		} catch (final InvalidCipherTextException e) {
			return null;
		}
	}

	private static IESEngine createIesEngine() {
		return new IESEngine(
				new ECDHBasicAgreement(),
				new KDF2BytesGenerator(new SHA1Digest()),
				new HMac(new SHA1Digest()));
	}

	// TODO 20141010 - we should probably move these to a utils class since Signer is using these too

	/**
	 * Gets the EC private key parameters.
	 *
	 * @return The EC private key parameters.
	 */
	private ECPrivateKeyParameters getPrivateKeyParameters(final PrivateKey privateKey) {
		return new ECPrivateKeyParameters(privateKey.getRaw(), SecP256K1Curve.secp256k1().getParams());
	}

	/**
	 * Gets the EC public key parameters.
	 *
	 * @return The EC public key parameters.
	 */
	private ECPublicKeyParameters getPublicKeyParameters(final PublicKey publicKey) {
		final ECPoint point = SecP256K1Curve.secp256k1().getParams().getCurve().decodePoint(publicKey.getRaw());
		return new ECPublicKeyParameters(point, SecP256K1Curve.secp256k1().getParams());
	}
}
