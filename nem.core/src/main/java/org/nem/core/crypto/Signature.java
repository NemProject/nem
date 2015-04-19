package org.nem.core.crypto;

import org.nem.core.serialization.*;
import org.nem.core.utils.*;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * A EC signature.
 */
public class Signature {
	private static final BigInteger MAXIMUM_VALUE = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);

	private final byte[] r;
	private final byte[] s;

	/**
	 * Creates a new signature.
	 *
	 * @param r The r-part of the signature.
	 * @param s The s-part of the signature.
	 */
	public Signature(final BigInteger r, final BigInteger s) {
		if (0 < r.compareTo(MAXIMUM_VALUE) || 0 < s.compareTo(MAXIMUM_VALUE)) {
			throw new IllegalArgumentException("r and s must fit into 32 bytes");
		}

		this.r = ArrayUtils.toByteArray(r, 32);
		this.s = ArrayUtils.toByteArray(s, 32);
	}

	/**
	 * Creates a new signature.
	 *
	 * @param bytes The binary representation of the signature.
	 */
	public Signature(final byte[] bytes) {
		if (64 != bytes.length) {
			throw new IllegalArgumentException("binary signature representation must be 64 bytes");
		}

		final byte[][] parts = ArrayUtils.split(bytes, 32);
		this.r = parts[0];
		this.s = parts[1];
	}

	/**
	 * Creates a new signature.
	 *
	 * @param r The binary representation of r.
	 * @param s The binary representation of s.
	 */
	public Signature(final byte[] r, final byte[] s) {
		if (32 != r.length || 32 != s.length) {
			throw new IllegalArgumentException("binary signature representation of r and s must both have 32 bytes length");
		}

		this.r = r;
		this.s = s;
	}

	/**
	 * Gets the r-part of the signature.
	 *
	 * @return The r-part of the signature.
	 */
	public BigInteger getR() {
		return ArrayUtils.toBigInteger(this.r);
	}

	/**
	 * Gets the r-part of the signature.
	 *
	 * @return The r-part of the signature.
	 */
	public byte[] getBinaryR() {
		return this.r;
	}

	/**
	 * Gets the s-part of the signature.
	 *
	 * @return The s-part of the signature.
	 */
	public BigInteger getS() {
		return ArrayUtils.toBigInteger(this.s);
	}

	/**
	 * Gets the s-part of the signature.
	 *
	 * @return The s-part of the signature.
	 */
	public byte[] getBinaryS() {
		return this.s;
	}

	/**
	 * Gets a little-endian 64-byte representation of the signature.
	 *
	 * @return a little-endian 64-byte representation of the signature
	 */
	public byte[] getBytes() {
		return ArrayUtils.concat(this.r, this.s);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.r) ^ Arrays.hashCode(this.s);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof Signature)) {
			return false;
		}

		final Signature rhs = (Signature)obj;
		return 1 == ArrayUtils.isEqualConstantTime(this.r, rhs.r) && 1 == ArrayUtils.isEqualConstantTime(this.s, rhs.s);
	}

	//region inline serialization

	/**
	 * Writes a signature object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param signature The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Signature signature) {
		serializer.writeBytes(label, signature.getBytes());
	}

	/**
	 * Reads a signature object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Signature readFrom(final Deserializer deserializer, final String label) {
		final byte[] bytes = deserializer.readBytes(label);
		return new Signature(bytes);
	}

	//endregion

	@Override
	public String toString() {
		return HexEncoder.getString(this.getBytes());
	}
}