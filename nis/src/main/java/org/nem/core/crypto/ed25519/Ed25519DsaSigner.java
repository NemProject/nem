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
		this.digest.update(hash, 32, 32); // only include the last 32 bytes of the private key hash
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
		// TODO 20141011 - why are we adding the public key to the hash?
		// > this implementation doesn't seem to be doing that https://github.com/jedisct1/libsodium/blob/master/src/libsodium/crypto_sign/ed25519/ref10/sign.c
		// > i understand the reason for adding the private key above (to add randomness to prevent against PS3-like attacks)
		// TODO 20141012 BR -> J: From the original paper (High-speed high-security signatures.pdf) where A is the public key:
		// TODO                  "the use of A is an inexpensive way to alleviate concerns that several public keys could be attacked simultaneously"
		// TODO                  The libsodium code does the same as this code, it just looks strange ^^ Or am I wrong?
		this.digest.update(this.keyPair.getPublicKey().getRaw());
		final Ed25519EncodedFieldElement h = new Ed25519EncodedFieldElement(this.digest.digest(data));
		final Ed25519EncodedFieldElement hModQ = h.modQ();
		final Ed25519EncodedFieldElement encodedS = hModQ.multiplyAndAddModQ(Ed25519Utils.prepareForScalarMultiply(this.keyPair.getPrivateKey().getRaw()), rModQ);

		// Signature is (encodedR, encodedS)
		final Signature signature = new Signature(encodedR.getRaw(), encodedS.getRaw());
		if (!this.isCanonicalSignature(signature)) {
			// TODO 20141011 - if we get here, this indicates a bug in our code?
			// TODO 20141012 BR -> J: yes, isCanonicalSignature checks 0 < encodedS < group order.
			// TODO                   encodedS was calculated mod group order so if it is bigger, something failed.
			// TODO                   I excluded encodedS == 0 as valid signature, not sure if that is needed.
			throw new CryptoException("Generated signature is not canonical");
		}

		return signature;
	}

	@Override
	public boolean verify(final byte[] data, final Signature signature) {
		// TODO 20141011 - does any of the validation here make sense:
		// https://github.com/jedisct1/libsodium/blob/master/src/libsodium/crypto_sign/ed25519/ref10/open.c
		// TODO 20141012 BR -> J: 1) First test is worse than isCanonicalSignature since it only checks bit 253-255 of S. It still could be > group order.
		// TODO                   2) Second test is same as "if (null == A) {" below.
		// TODO                   3) Third test checks if the raw value of the public key is 0. In that case A is the affine point (+-i, 0).
		// TODO                      Does this cause problems? hmmm...need to think about it.

		if (!this.isCanonicalSignature(signature)) {
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
