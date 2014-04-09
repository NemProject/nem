package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.util.Arrays;

/**
 * A hash.
 */
public class Hash implements SerializableEntity {

	private byte[] data;

	/**
	 * An object deserializer that can be used to deserialize hashes.
	 */
	public static final ObjectDeserializer<Hash> DESERIALIZER = new ObjectDeserializer<Hash>() {
		@Override
		public Hash deserialize(final Deserializer deserializer) {
			return new Hash(deserializer);
		}
	};

	/**
	 * Creates new Hash object.
	 *
	 * @param data The raw hash code.
	 */
	public Hash(byte[] data) {
		this.data = data;
	}

	/**
	 * Deserializes a hash code object.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public Hash(Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	/**
	 * Gets the raw hash code.
	 *
	 * @return The raw hash code.
	 */
	public byte[] getRaw() { return this.data; }

	@Override
	public void serialize(Serializer serializer) {
		serializer.writeBytes("data", this.getRaw());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Hash))
			return false;

		Hash rhs = (Hash)obj;
		return Arrays.equals(this.data, rhs.data);
	}

	@Override
	public String toString() {
		return HexEncoder.getString(this.data);
	}
}
