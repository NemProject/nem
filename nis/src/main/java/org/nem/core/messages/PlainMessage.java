package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * A plain, unencrypted message.
 */
public class PlainMessage extends Message {

    final byte[] payload;

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
    public boolean canDecode() { return true; }

    @Override
    public byte[] getEncodedPayload() { return this.payload; }

    @Override
    public byte[] getDecodedPayload() { return this.payload; }

    @Override
    public void serialize(final Serializer serializer) {
        super.serialize(serializer);
        serializer.writeBytes("payload", this.payload);
    }

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PlainMessage)) {
			return false;
		}

		PlainMessage rhs = (PlainMessage)obj;
		return this.payload.equals(rhs.payload);
	}

	@Override
	public int hashCode() {
		return this.payload.hashCode();
	}
}
