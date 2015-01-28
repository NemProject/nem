package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;

/**
 * Factory class that can deserialize all known messages.
 */
public class MessageFactory {

	/**
	 * Deserializes a message.
	 *
	 * @param deserializer The deserializer.
	 * @param sender The message sender.
	 * @param recipient The message recipient.
	 * @return The deserialized message.
	 */
	public static Message deserialize(final Deserializer deserializer, final Account sender, final Account recipient) {
		final int type = deserializer.readInt("type");

		switch (type) {
			case MessageTypes.PLAIN:
				return new PlainMessage(deserializer);

			case MessageTypes.SECURE:
				return new SecureMessage(deserializer, sender, recipient);
		}

		throw new IllegalArgumentException("Unknown message type: " + type);
	}
}
