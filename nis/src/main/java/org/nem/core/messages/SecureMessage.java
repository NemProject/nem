package org.nem.core.messages;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * A secure, encrypted message.
 */
public class SecureMessage extends Message {

    final Account sender;
    final Account recipient;
    final byte[] payload;

    /**
     * Creates a new secure message.
     *
     * @param sender The message sender.
     * @param recipient The message recipient.
     * @param payload The unencrypted payload.
     */
    public SecureMessage(final Account sender, final Account recipient, final byte[] payload) {
        super(MessageTypes.SECURE);
        this.sender = sender;
        this.recipient = recipient;

        if (!sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("sender private key is required for creating secure message");

        final Cipher cipher = new Cipher(this.sender.getKeyPair(), this.recipient.getKeyPair());
        this.payload = cipher.encrypt(payload);
    }

    /**
     * Deserializes a secure message.
     *
     * @param deserializer The deserializer.
     */
    public SecureMessage(final Deserializer deserializer) {
        super(MessageTypes.SECURE);
        this.sender = SerializationUtils.readAccount(deserializer, "sender");
        this.recipient = SerializationUtils.readAccount(deserializer, "recipient");
        this.payload = deserializer.readBytes("payload");
    }

    @Override
    public boolean canDecode() {
        return this.recipient.getKeyPair().hasPrivateKey();
    }

    @Override
    public byte[] getEncodedPayload() {
        return this.payload;
    }

    @Override
    public byte[] getDecodedPayload() {
        if (!this.recipient.getKeyPair().hasPrivateKey())
            return null;

        final Cipher cipher = new Cipher(this.sender.getKeyPair(), this.recipient.getKeyPair());
        return cipher.decrypt(this.payload);
    }

    @Override
    public void serialize(final Serializer serializer) {
        super.serialize(serializer);
        SerializationUtils.writeAccount(serializer, "sender", this.sender);
        SerializationUtils.writeAccount(serializer, "recipient", this.recipient);
        serializer.writeBytes("payload", this.payload);
    }
}

