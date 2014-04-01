package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Represents a prepare request.
 */
public class RequestPrepare implements SerializableEntity {
	private byte[] data;

	/**
	 * Creates a new request.
	 *
	 * @param data The data.
	 */
	public RequestPrepare(final byte[] data) {
		this.data = data;
	}

	/**
	 * Deserializes a request.
	 *
	 * @param deserializer The deserializer.
	 */
	public RequestPrepare(Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	/**
	 * Gets the request data.
	 *
	 * @return The request data.
	 */
	public byte[] getData() {
		return data;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.getData());
	}
}
