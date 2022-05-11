package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Base class for all messages.
 */
public abstract class Message implements SerializableEntity {

	private final int type;

	/**
	 * Creates a new message.
	 *
	 * @param type The message type.
	 */
	protected Message(final int type) {
		this.type = type;
	}

	/**
	 * Gets the type.
	 *
	 * @return The type.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Determines if this message can be decoded.
	 *
	 * @return true if this message can be decoded.
	 */
	public abstract boolean canDecode();

	/**
	 * Gets the encoded message payload.
	 *
	 * @return The encoded message.
	 */
	public abstract byte[] getEncodedPayload();

	/**
	 * Gets the decoded message payload.
	 *
	 * @return The decoded message.
	 */
	public abstract byte[] getDecodedPayload();

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("type", this.type);
	}
}
