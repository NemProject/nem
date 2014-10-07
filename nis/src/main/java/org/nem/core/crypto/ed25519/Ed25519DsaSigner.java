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
	private final Ed25519ScalarOps scalarOps;

	/**
	 * Creates a Ed25519 DSA signer.
	 *
	 *
	 *
	 * @param keyPair The key pair to use.
	 */
	public Ed25519DsaSigner(final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.digest = Hashes.getSha3_512Instance();
		this.scalarOps = new Ed25519ScalarOps();
	}

	@Override
	public Signature sign(final byte[] data) {
		if (!this.keyPair.hasPrivateKey()) {
			throw new CryptoException("cannot sign without private key");
		}

		// Hash the private key to improve randomness.
		final byte[] hash = this.digest.digest(ArrayUtils.toByteArray(keyPair.getPrivateKey().getRaw(), 32));
		this.digest.reset();

		// r = H(hash_b,...,hash_2b-1, data) where b=256.
		this.digest.update(hash, 32, 32);
		byte[] r = this.digest.digest(data);

		// Reduce size of r since we are calculating mod group order anyway
		r = this.scalarOps.reduce(r);

		// R = r * base point.
		final GroupElement R = Ed25519Constants.basePoint.scalarMultiply(r);
		final byte[] encodedR = R.toByteArray();

		// S = (r + H(encodedR, encodedA, data) * a) mod group order where
		// encodedR and encodedA are the little endian encodings of the group element R and the public key A and
		// a is the lower 32 bytes of hash after clamping.
		this.digest.update(encodedR);
		final byte[] encodedA = this.keyPair.getPublicKey().getRaw();
		this.digest.update(encodedA);
		byte[] h = this.digest.digest(data);
		h = this.scalarOps.reduce(h);
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		clamp(a);
		final byte[] encodedS = this.scalarOps.multiplyAndAdd(h, a, r);

		// Signature is (encodedR, encodedS)
		final Signature signature = new Signature(encodedR, encodedS);
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
		final byte[] encodedR = signature.getBinaryR();
		final byte[] encodedA = this.keyPair.getPublicKey().getRaw();
		this.digest.update(encodedR);
		this.digest.update(encodedA);
		final byte[] h = this.digest.digest(data);

		// hReduced = h mode group order
		final byte[] hReduced = this.scalarOps.reduce(h);

		final Field field = Ed25519Constants.curve.getField();
		final GroupElement minusA = GroupElement.p3(
				Ed25519Constants.curve,
				new Ed25519FieldElement(field, this.keyPair.getPublicKey().getX()),
				new Ed25519FieldElement(field, this.keyPair.getPublicKey().getY()),
				new Ed25519FieldElement(field, this.keyPair.getPublicKey().getZ()),
				new Ed25519FieldElement(field, this.keyPair.getPublicKey().getT())
				).negate();
		minusA.precompute(false);

		// R = encodedS * B + H(encodedR, encodedA, data) * minusA
		final GroupElement calculatedR = Ed25519Constants.basePoint.doubleScalarMultiplyVariableTime(
				minusA, hReduced, signature.getBinaryS());

		// Compare calculated R to given R.
		final byte[] encodedCalculatedR = calculatedR.toByteArray();
		final int result = Utils.equal(encodedCalculatedR, encodedR);
		return 1 == result;
	}

	@Override
	public boolean isCanonicalSignature(final Signature signature) {
		return -1 == signature.getS().compareTo(Ed25519Constants.groupOrder) &&
				1 == signature.getS().compareTo(BigInteger.ZERO);
	}

	@Override
	public Signature makeSignatureCanonical(final Signature signature) {
		final Ed25519ScalarOps scalarOps = new Ed25519ScalarOps();
		final byte[] s = Arrays.copyOf(signature.getBinaryS(), 64);
		final byte[] sReduced = scalarOps.reduce(s);

		return new Signature(signature.getBinaryR(), sReduced);
	}

	private void clamp(byte[] k) {
		k[31] &= 0x7F;
		k[31] |= 0x40;
		k[0] &= 0xF8;
	}
}
