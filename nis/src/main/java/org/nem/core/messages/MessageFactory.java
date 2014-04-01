package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Factory class that can deserialize all known messages.
 */
public class MessageFactory {

	/**
	 * Creates an object deserializer that is able to deserialize messages
	 * sent from sender to recipient.
	 *
	 * @param sender    The message sender.
	 * @param recipient The message recipient.
	 *
	 * @return An object deserializer.
	 */
	public static ObjectDeserializer<Message> createDeserializer(final Account sender, final Account recipient) {
		return new ObjectDeserializer<Message>() {
			@Override
			public Message deserialize(final Deserializer deserializer) {
				return MessageFactory.deserialize(sender, recipient, deserializer);
			}
		};
	}

	/**
	 * Deserializes a message.
	 *
	 * @param sender       The message sender.
	 * @param recipient    The message recipient.
	 * @param deserializer The deserializer.
	 *
	 * @return The deserialized message.
	 */
	public static Message deserialize(final Account sender, final Account recipient, final Deserializer deserializer) {
		int type = deserializer.readInt("type");

		switch (type) {
			case MessageTypes.PLAIN:
				return new PlainMessage(deserializer);

			case MessageTypes.SECURE:
				return new SecureMessage(sender, recipient, deserializer);
		}

		throw new InvalidParameterException("Unknown message type: " + type);
	}
}
