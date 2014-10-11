package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;
import org.nem.core.crypto.ed25519.arithmetic.*;
import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Implementation of the DSA signer for Ed25519.
 */
public class Ed25519DsaSigner implements DsaSigner {

	private final KeyPair keyPair;
	private final MessageDigest digest;

	/**
	 * Creates a Ed25519 DSA signer.
	 *
	 * @param keyPair The key pair to use.
	 */
	public Ed25519DsaSigner(final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.digest = Hashes.getSha3_512Instance();
	}

	@Override
	public Signature sign(final byte[] data) {
		if (!this.keyPair.hasPrivateKey()) {
			throw new CryptoException("cannot sign without private key");
		}

		// Hash the private key to improve randomness.
		final byte[] hash = this.digest.digest(ArrayUtils.toByteArray(this.keyPair.getPrivateKey().getRaw(), 32));

		// r = H(hash_b,...,hash_2b-1, data) where b=256.
		this.digest.update(hash, 32, 32);
		final Ed25519EncodedFieldElement r = new Ed25519EncodedFieldElement(this.digest.digest(data));

		// Reduce size of r since we are calculating mod group order anyway
		final Ed25519EncodedFieldElement rModQ = r.modQ();

		// R = rModQ * base point.
		final Ed25519GroupElement R = Ed25519Group.BASE_POINT.scalarMultiply(rModQ);
		final Ed25519EncodedGroupElement encodedR = R.encode();

		// S = (r + H(encodedR, encodedA, data) * a) mod group order where
		// encodedR and encodedA are the little endian encodings of the group element R and the public key A and
		// a is the lower 32 bytes of hash after clamping.
		this.digest.update(encodedR.getRaw());
		this.digest.update(this.keyPair.getPublicKey().getRaw());
		final Ed25519EncodedFieldElement h = new Ed25519EncodedFieldElement(this.digest.digest(data));
		final Ed25519EncodedFieldElement hModQ = h.modQ();
		final Ed25519EncodedFieldElement encodedS = hModQ.multiplyAndAddModQ(this.keyPair.getPrivateKey().prepareForScalarMultiply(), rModQ);

		// Signature is (encodedR, encodedS)
		final Signature signature = new Signature(encodedR.getRaw(), encodedS.getRaw());
		if (!isCanonicalSignature(signature)) {
			throw new CryptoException("Generated signature is not canonical");
		}

		return signature;
	}

	@Override
	public boolean verify(final byte[] data, final Signature signature) {
		if (!isCanonicalSignature(signature)) {
			return false;
		}

		// h = H(encodedR, encodedA, data).
		final byte[] rawEncodedR = signature.getBinaryR();
		final byte[] rawEncodedA = this.keyPair.getPublicKey().getRaw();
		this.digest.update(rawEncodedR);
		this.digest.update(rawEncodedA);
		final Ed25519EncodedFieldElement h = new Ed25519EncodedFieldElement(this.digest.digest(data));

		// hReduced = h mod group order
		final Ed25519EncodedFieldElement hModQ = h.modQ();

		Ed25519GroupElement A = this.keyPair.getPublicKey().getAsGroupElement();
		if (null == A) {
			// Must compute A.
			A = new Ed25519EncodedGroupElement(rawEncodedA).decode();
			A.precomputeForDoubleScalarMultiplication();
		}

		// R = encodedS * B - H(encodedR, encodedA, data) * A
		final Ed25519GroupElement calculatedR = Ed25519Group.BASE_POINT.doubleScalarMultiplyVariableTime(
				A, hModQ, new Ed25519EncodedFieldElement(signature.getBinaryS()));

		// Compare calculated R to given R.
		final byte[] encodedCalculatedR = calculatedR.encode().getRaw();
		final int result = ArrayUtils.isEqual(encodedCalculatedR, rawEncodedR);
		return 1 == result;
	}

	@Override
	public boolean isCanonicalSignature(final Signature signature) {
		return -1 == signature.getS().compareTo(Ed25519Group.GROUP_ORDER) &&
				1 == signature.getS().compareTo(BigInteger.ZERO);
	}

	@Override
	public Signature makeSignatureCanonical(final Signature signature) {
		final Ed25519EncodedFieldElement s = new Ed25519EncodedFieldElement(Arrays.copyOf(signature.getBinaryS(), 64));
		final Ed25519EncodedFieldElement sModQ = s.modQ();

		return new Signature(signature.getBinaryR(), sModQ.getRaw());
	}
}
