package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.util.Arrays;

/**
 * A plain, unencrypted message.
 */
public class PlainMessage extends Message {

	private final byte[] payload;

	/**
	 * Creates a new plain message.
	 *
	 * @param payload The unencrypted payload.
	 */
	public PlainMessage(final byte[] payload) {
		super(MessageTypes.PLAIN);
		this.payload = payload;
	}

	/**
	 * Deserializes a plain message.
	 *
	 * @param deserializer The deserializer.
	 */
	public PlainMessage(final Deserializer deserializer) {
		super(MessageTypes.PLAIN);
		this.payload = deserializer.readBytes("payload");
	}

	@Override
	public boolean canDecode() {
		return true;
	}

	@Override
	public byte[] getEncodedPayload() {
		return this.payload;
	}

	@Override
	public byte[] getDecodedPayload() {
		return this.payload;
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeBytes("payload", this.payload);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.payload);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof PlainMessage)) {
			return false;
		}

		final PlainMessage rhs = (PlainMessage)obj;
		return Arrays.equals(this.payload, rhs.payload);
	}
}
