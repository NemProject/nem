package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.SerializationUtils;
import org.nem.core.serialization.Serializer;
import org.nem.core.utils.ByteUtils;

import java.util.Arrays;

/**
 * Wrapper for array of bytes
 */
public class ByteArray implements SerializableEntity {
	private byte[] data;

	/**
	 * Creates new ByteArray object
	 *
	 * @param data data that will be held.
	 */
	public ByteArray(byte[] data) {
		this.data = data;
	}

	/**
	 * Deserializes byte array object
	 *
	 * @param deserializer The deserializer to use.
	 */
	public ByteArray(Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	/**
	 * Gets data
	 *
	 * @return data
	 */
	public byte[] get() {
		return data;
	}

	@Override
	public void serialize(Serializer serializer) {
		serializer.writeBytes("data", this.get());
	}

	@Override
	public int hashCode() {
		return ByteUtils.bytesToInt(this.data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ByteArray))
			return false;

		ByteArray rhs = (ByteArray)obj;
		return Arrays.equals(this.data, rhs.data);
	}
}
