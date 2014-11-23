package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * Represents a prepare request.
 */
public class RequestPrepare implements SerializableEntity {
	private final byte[] data;

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
	public RequestPrepare(final Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	/**
	 * Gets the request data.
	 *
	 * @return The request data.
	 */
	public byte[] getData() {
		return this.data;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.getData());
	}
}
