package org.nem.core.messages;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

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

    /**
     * Deserializes a message.
     *
     * @param deserializer The deserializer.
     * @return The deserialized message.
     */
    public static Message deserialize(Deserializer deserializer) {
        int type = deserializer.readInt("type");

        switch (type) {
            case MessageTypes.PLAIN:
                return new PlainMessage(deserializer);

            case MessageTypes.SECURE:
                return new SecureMessage(deserializer);
        }

        throw new InvalidParameterException("Unknown message type: " + type);
    }
}
