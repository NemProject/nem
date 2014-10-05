package org.nem.core.crypto;

import org.nem.core.serialization.*;
import org.nem.core.utils.*;

import java.math.BigInteger;

/**
 * A EC signature.
 */
public class Signature {

	private final BigInteger r;
	private BigInteger s;

	/**
	 * Creates a new signature.
	 *
	 * @param r The r-part of the signature.
	 * @param s The s-part of the signature.
	 */
	public Signature(final BigInteger r, final BigInteger s) {
		this.r = r;
		this.s = s;
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
		this.r = ArrayUtils.toBigInteger(parts[0]);
		this.s = ArrayUtils.toBigInteger(parts[1]);
	}

	/**
	 * Gets the r-part of the signature.
	 *
	 * @return The r-part of the signature.
	 */
	public BigInteger getR() {
		return this.r;
	}

	/**
	 * Gets the s-part of the signature.
	 *
	 * @return The s-part of the signature.
	 */
	public BigInteger getS() {
		return this.s;
	}

	/**
	 * Gets a little-endian 64-byte representation of the signature.
	 *
	 * @return a little-endian 64-byte representation of the signature
	 */
	public byte[] getBytes() {
		final byte[] rBytes = ArrayUtils.toByteArray(this.r, 32);
		final byte[] sBytes = ArrayUtils.toByteArray(this.s, 32);
		return ArrayUtils.concat(rBytes, sBytes);
	}

	@Override
	public int hashCode() {
		return this.r.hashCode() ^ this.s.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof Signature)) {
			return false;
		}

		final Signature rhs = (Signature)obj;
		return 0 == this.r.compareTo(rhs.r) && 0 == this.s.compareTo(rhs.s);
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