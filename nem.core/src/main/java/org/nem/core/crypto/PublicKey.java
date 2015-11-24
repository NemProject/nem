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
	 * Creates a new public key.
	 *
	 * @param bytes The raw public key value.
	 */
	public PublicKey(final byte[] bytes) {
		this.value = bytes;
	}

	/**
	 * Deserializes a public key.
	 *
	 * @param deserializer The deserializer.
	 */
	public PublicKey(final Deserializer deserializer) {
		this.value = deserializer.readBytes("value");
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
