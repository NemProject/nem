package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.Signature;
import org.nem.core.crypto.ed25519.arithmetic.*;

import java.math.BigInteger;
import java.security.*;
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
		try {
			this.digest = MessageDigest.getInstance("SHA3-256", "BC");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new CryptoException(e);
		}
	}

	@Override
	public Signature sign(final byte[] data) {
		// Hash the private key to improve randomness.
		final byte[] hash = this.digest.digest(Utils.toByteArray(keyPair.getPrivateKey().getRaw()));
		this.digest.reset();

		// r = H(hash_b,...,hash_2b-1, data) where b=256.
		this.digest.update(hash, 32, 32);
		byte[] r = this.digest.digest(data);

		// Reduce size of r since we are calculating mod group order anyway
		final Ed25519ScalarOps scalarOps = new Ed25519ScalarOps();
		r = scalarOps.reduce(r);

		// R = r * base point.
		final GroupElement R = Ed25519Constants.basePoint.scalarMultiply(r);
		final byte[] encodedR = R.toByteArray();

		// S = (r + H(encodedR, encodedA, data) * a) mod group order where
		// encodedR and encodedA are the little endian encodings of the group element R and the public key A and
		// a is the lower 32 bytes of hash after clamping.
		this.digest.update(encodedR);
		// TODO 20141005 BR: is it guaranteed that the public key is available or do we have to calculate it eventually?
		final byte[] encodedA = this.keyPair.getPublicKey().getRaw();
		this.digest.update(encodedA);
		byte[] h = this.digest.digest(data);
		h = scalarOps.reduce(h);
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		clamp(a);
		final byte[] encodedS = scalarOps.multiplyAndAdd(h, a, r);

		// Signature is (RCompressed, S)
		final Signature signature = new Signature(Utils.toBigInteger(encodedR), Utils.toBigInteger(encodedS));
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
		final byte[] encodedR = Utils.toByteArray(signature.getR());
		// TODO 20141005 BR: is it guaranteed that the public key is available or do we have to calculate it eventually?
		final byte[] encodedA = this.keyPair.getPublicKey().getRaw();
		final GroupElement A = new GroupElement(Ed25519Constants.curve, encodedA);
		this.digest.update(encodedR);
		this.digest.update(encodedA);
		final byte[] h = this.digest.digest(data);

		// hReduced = h mode group order
		final Ed25519ScalarOps scalarOps = new Ed25519ScalarOps();
		final byte[] hReduced = scalarOps.reduce(h);

		// R = encodedS * B - H(encodedR, encodedA, data) * a * B
		final byte[] encodedS = Utils.toByteArray(signature.getS());
		final GroupElement calculatedR = Ed25519Constants.basePoint.doubleScalarMultiplyVariableTime(
				A.negate(), hReduced, encodedS);

		// Compare calculated R to given R.
		final byte[] encodedCalculatedR = calculatedR.toByteArray();
		final int result = Utils.equal(encodedCalculatedR, encodedR);
		return 1 == result;
	}

	@Override
	public boolean isCanonicalSignature(final Signature signature) {
		return 0 <= signature.getS().compareTo(Ed25519Constants.groupOrder) ||
				-1 == signature.getS().compareTo(BigInteger.ZERO);
	}

	@Override
	public Signature makeSignatureCanonical(final Signature signature) {
		final Ed25519ScalarOps scalarOps = new Ed25519ScalarOps();
		final byte[] encodedS = Utils.toByteArray(signature.getS());
		final byte[] encodedSReduced = scalarOps.reduce(encodedS);

		return new Signature(signature.getR(), Utils.toBigInteger(encodedSReduced));
	}

	private void clamp(byte[] k) {
		k[31] &= 0x7F;
		k[31] |= 0x40;
		k[ 0] &= 0xF8;
	}
}
