package org.nem.core.crypto;

import org.nem.core.crypto.ed25519.arithmetic.Ed25519GroupElement;
import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.util.Arrays;

/**
 * Represents a public key.
 */
public class PublicKey implements SerializableEntity {

	private final byte[] value;

	/**
	 * The following field is used by Ed25519 to speed up verification.
	 */
	private final Ed25519GroupElement A;

	/**
	 * Creates a new public key.
	 *
	 * @param bytes The raw public key value.
	 */
	public PublicKey(final byte[] bytes) {
		this.value = bytes;
		this.A = null;
	}

	/**
	 * Creates a new public key.
	 *
	 * @param bytes The raw public key value.
	 * @param A The corresponding group element.
	 */
	public PublicKey(
			final byte[] bytes,
			final Ed25519GroupElement A) {
		this.value = bytes;
		this.A = A;

		if (null == A || !A.isPrecomputedForDoubleScalarMultiplication()) {
			throw new RuntimeException("A not prepared for double scalar multiplication.");
		}
	}

	/**
	 * Deserializes a public key.
	 *
	 * @param deserializer The deserializer.
	 */
	public PublicKey(final Deserializer deserializer) {
		this.value = deserializer.readBytes("value");
		this.A = null;
	}

	/**
	 * Creates a public key from a hex string.
	 *
	 * @param hex The hex string.
	 * @return The new public key.
	 */
	public static PublicKey fromHexString(final String hex) {
		try {
			return new PublicKey(HexEncoder.getBytes(hex));
		} catch (final IllegalArgumentException e) {
			throw new CryptoException(e);
		}
	}

	/**
	 * Gets the public key as group element (can return null).
	 *
	 * @return The group element or null if not set.
	 */
	public Ed25519GroupElement getAsGroupElement() {
		return this.A;
	}

	/**
	 * Gets the raw public key value.
	 *
	 * @return The raw public key value.
	 */
	public byte[] getRaw() {
		return this.value;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("value", this.value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof PublicKey)) {
			return false;
		}

		final PublicKey rhs = (PublicKey)obj;
		return Arrays.equals(this.value, rhs.value);
	}

	@Override
	public String toString() {
		return HexEncoder.getString(this.value);
	}
}
