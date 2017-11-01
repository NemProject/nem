package org.nem.core.crypto.secp256k1;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.*;
import org.bouncycastle.math.ec.ECPoint;
import org.nem.core.crypto.*;

import java.math.BigInteger;

/**
 * Implementation of the DSA signer for SECP256K1.
 */
public class SecP256K1DsaSigner implements DsaSigner {
	private final KeyPair keyPair;

	/**
	 * Creates a SECP256K1 DSA signer.
	 *
	 * @param keyPair The key pair to use.
	 */
	public SecP256K1DsaSigner(final KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	@Override
	public Signature sign(final byte[] data) {
		if (!this.keyPair.hasPrivateKey()) {
			throw new CryptoException("cannot sign without private key");
		}

		final ECDSASigner signer = this.createECDSASigner();
		final ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(
				this.keyPair.getPrivateKey().getRaw(),
				SecP256K1Curve.secp256k1().getParams());
		signer.init(true, privateKeyParameters);
		final byte[] hash = Hashes.sha3_256(data);
		final BigInteger[] components = signer.generateSignature(hash);
		final Signature signature = new Signature(components[0], components[1]);
		return this.makeSignatureCanonical(signature);
	}

	@Override
	public boolean verify(final byte[] data, final Signature signature) {
		if (!this.isCanonicalSignature(signature)) {
			return false;
		}

		final ECDSASigner signer = this.createECDSASigner();
		final ECPoint point = SecP256K1Curve.secp256k1().getParams().getCurve().decodePoint(this.keyPair.getPublicKey().getRaw());
		final ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(point, SecP256K1Curve.secp256k1().getParams());
		signer.init(false, publicKeyParameters);
		final byte[] hash = Hashes.sha3_256(data);
		return signer.verifySignature(hash, signature.getR(), signature.getS());
	}

	@Override
	public boolean isCanonicalSignature(final Signature signature) {
		return signature.getS().compareTo(SecP256K1Curve.secp256k1().getHalfGroupOrder()) <= 0;
	}

	@Override
	public Signature makeSignatureCanonical(final Signature signature) {
		return this.isCanonicalSignature(signature)
				? signature
				: new Signature(signature.getR(), SecP256K1Curve.secp256k1().getParams().getN().subtract(signature.getS()));
	}

	private ECDSASigner createECDSASigner() {
		return new ECDSASigner(new HMacDSAKCalculator(new KeccakDigest(256)));
	}
}
