package org.nem.core.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;

import java.math.BigInteger;

/**
 * Wraps EC DSA signing and verification logic.
 */
public class Signer {

	private final KeyPair keyPair;

	/**
	 * Creates a signer around a KeyPair.
	 *
	 * @param keyPair The KeyPair that should be used for signing and verification.
	 */
	public Signer(final KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	/**
	 * Signs the SHA3 hash of an arbitrarily sized message.
	 *
	 * @param data The message to sign.
	 *
	 * @return The generated signature.
	 */
	public Signature sign(final byte[] data) {
		ECDSASigner signer = createECDSASigner();
		signer.init(true, this.keyPair.getPrivateKeyParameters());
		final byte[] hash = Hashes.sha3(data);
		BigInteger[] components = signer.generateSignature(hash);
		final Signature signature = new Signature(components[0], components[1]);
		signature.makeCanonical();
		return signature;
	}

	/**
	 * Verifies that the signature is valid.
	 *
	 * @param data      The original message.
	 * @param signature The generated signature.
	 *
	 * @return true if the signature is valid.
	 */
	public boolean verify(final byte[] data, final Signature signature) {
		if (!signature.isCanonical())
			return false;

		ECDSASigner signer = createECDSASigner();
		signer.init(false, this.keyPair.getPublicKeyParameters());
		final byte[] hash = Hashes.sha3(data);
		return signer.verifySignature(hash, signature.getR(), signature.getS());
	}

	private ECDSASigner createECDSASigner() {
		return new ECDSASigner(new HMacDSAKCalculator(new SHA3Digest(256)));
	}
}
