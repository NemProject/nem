package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Factory class that can deserialize all known messages.
 */
public class MessageFactory {

	/**
	 * An object deserializer that wraps this factory.
	 */
	public static final ObjectDeserializer<Message> DESERIALIZER = new ObjectDeserializer<Message>() {
		@Override
		public Message deserialize(Deserializer deserializer) {
			return MessageFactory.deserialize(deserializer);
		}
	};

	private static Message deserialize(final Deserializer deserializer) {
		int type = deserializer.readInt("type");

		switch (type) {
			case MessageTypes.PLAIN:
				return new PlainMessage(deserializer);

			case MessageTypes.SECURE:
				return new SecureMessage(deserializer);
		}

		throw new IllegalArgumentException("Unknown message type: " + type);
	}
}
