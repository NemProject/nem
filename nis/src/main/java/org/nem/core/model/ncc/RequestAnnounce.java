package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * Represents an announce request.
 */
public class RequestAnnounce implements SerializableEntity {
	private final byte[] data;
	private final byte[] signature;

	/**
	 * Creates a new request.
	 *
	 * @param data The data.
	 * @param signature The signature.
	 */
	public RequestAnnounce(final byte[] data, final byte[] signature) {
		this.data = data;
		this.signature = signature;
	}

	/**
	 * Deserializes a request.
	 *
	 * @param deserializer The deserializer.
	 */
	public RequestAnnounce(final Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
		this.signature = deserializer.readBytes("signature");
	}

	/**
	 * Gets the request data.
	 *
	 * @return The request data.
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * Gets the request signature.
	 *
	 * @return The request signature.
	 */
	public byte[] getSignature() {
		return this.signature;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.getData());
		serializer.writeBytes("signature", this.getSignature());
	}
}
