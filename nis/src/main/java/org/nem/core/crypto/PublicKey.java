package org.nem.core.crypto;

import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.util.Arrays;

/**
 * Represents a public key.
 */
public class PublicKey implements SerializableEntity {

	private final byte[] value;

	/**
	 * The following fields are used by Ed25519 to speed up verification.
	 */
	private final int[] X;
	private final int[] Y;
	private final int[] Z;
	private final int[] T;

	/**
	 * Creates a new public key.
	 *
	 * @param bytes The raw public key value.
	 */
	public PublicKey(final byte[] bytes) {
		this.value = bytes;
		this.X = null;
		this.Y = null;
		this.Z = null;
		this.T = null;
	}

	/**
	 * Creates a new public key.
	 *
	 * @param bytes The raw public key value.
	 * @param X The projective X coordinate.
	 * @param Y The projective Y coordinate.
	 * @param Z The projective Z coordinate.
	 * @param T The projective T coordinate.
	 */
	public PublicKey(
			final byte[] bytes,
			final int[] X,
			final int[] Y,
			final int[] Z,
			final int[] T) {
		this.value = bytes;

		if (null == X ||
			null == Y ||
			null == Z ||
			null == T ||
			10 != X.length ||
			10 != Y.length ||
			10 != Z.length ||
			10 != T.length) {
			throw new RuntimeException("Projective coordinate has wrong array length.");
		}
		this.X = X;
		this.Y = Y;
		this.Z = Z;
		this.T = T;
	}

	/**
	 * Deserializes a public key.
	 *
	 * @param deserializer The deserializer.
	 */
	public PublicKey(final Deserializer deserializer) {
		this.value = deserializer.readBytes("value");
		this.X = null;
		this.Y = null;
		this.Z = null;
		this.T = null;
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
	 * Gets the projective X coordinate.
	 *
	 * @return The X coordinate.
	 */
	public int[] getX() {
		if (null == this.X) {
			throw new RuntimeException("Projective coordinate not set.");
		}
		return this.X;
	}

	/**
	 * Gets the projective Y coordinate.
	 *
	 * @return The Y coordinate.
	 */
	public int[] getY() {
		if (null == this.Y) {
			throw new RuntimeException("Projective coordinate not set.");
		}
		return this.Y;
	}

	/**
	 * Gets the projective Z coordinate.
	 *
	 * @return The Z coordinate.
	 */
	public int[] getZ() {
		if (null == this.Z) {
			throw new RuntimeException("Projective coordinate not set.");
		}
		return this.Z;
	}

	/**
	 * Gets the projective T coordinate.
	 *
	 * @return The T coordinate.
	 */
	public int[] getT() {
		if (null == this.T) {
			throw new RuntimeException("Projective coordinate not set.");
		}
		return this.T;
	}

	/**
	 * Gets the raw public key value.
	 *
	 * @return The raw public key value.
	 */
	public byte[] getRaw() {
		return this.value;
	}

	/**
	 * Determines if the public key is in compressed form.
	 *
	 * @return true if the public key is in compressed form.
	 */
	public boolean isCompressed() {
		return CryptoEngines.getDefaultEngine().createKeyAnalyzer().isKeyCompressed(this);
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
