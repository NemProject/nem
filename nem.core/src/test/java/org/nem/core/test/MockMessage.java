package org.nem.core.test;

import org.nem.core.model.Message;
import org.nem.core.serialization.*;

/**
 * A mock Message implementation.
 */
public class MockMessage extends Message {

	public static final int TYPE = 19;

	private final int customField;
	private byte[] encodedPayload;
	private byte[] decodedPayload;

	/**
	 * Creates a mock message.
	 *
	 * @param customField The initial custom field value.
	 */
	public MockMessage(final int customField) {
		super(TYPE);
		this.customField = customField;
	}

	/**
	 * Deserializes a mock verifiable entity.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public MockMessage(final Deserializer deserializer) {
		super(deserializer.readInt("type"));
		this.customField = deserializer.readInt("customField");
	}

	/**
	 * Gets the custom field value.
	 *
	 * @return The custom field value.
	 */
	public int getCustomField() {
		return this.customField;
	}

	/**
	 * Sets the encoded payload.
	 *
	 * @param payload The desired encoded payload.
	 */
	public void setEncodedPayload(final byte[] payload) {
		this.encodedPayload = payload;
	}

	/**
	 * Sets the decoded payload.
	 *
	 * @param payload The desired decoded payload.
	 */
	public void setDecodedPayload(final byte[] payload) {
		this.decodedPayload = payload;
	}

	@Override
	public boolean canDecode() {
		return true;
	}

	@Override
	public byte[] getEncodedPayload() {
		return this.encodedPayload;
	}

	@Override
	public byte[] getDecodedPayload() {
		return this.decodedPayload;
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeInt("customField", this.customField);
	}
}