package org.nem.core.connect;

import org.nem.core.serialization.*;

/**
 * Creates a new binary HTTP POST request.
 */
public class HttpBinaryPostRequest implements HttpPostRequest {
	private final byte[] entityBytes;

	/**
	 * Creates a new request.
	 *
	 * @param entity The entity.
	 */
	public HttpBinaryPostRequest(final SerializableEntity entity) {
		this.entityBytes = BinarySerializer.serializeToBytes(entity);
	}

	@Override
	public byte[] getPayload() {
		return this.entityBytes;
	}

	@Override
	public String getContentType() {
		return ContentType.BINARY;
	}
}