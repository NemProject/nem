package org.nem.core.model;

import org.nem.core.crypto.*;

/**
 * A message sent from one account to another.
 */
public class Message {
    private final KeyPair recipientKeyPair;
    private final byte[] senderPublicKey;
    private final byte[] message;

    /**
     * Creates a new Message.
     *
     * @param recipientKeyPair The recipient key pair.
     * @param senderPublicKey The sender's public key.
     * @param message The raw message.
     */
    public Message(final KeyPair recipientKeyPair, final byte[] senderPublicKey, byte[] message) {
        this.recipientKeyPair = recipientKeyPair;
        this.senderPublicKey = senderPublicKey;
        this.message = message;
    }

    /**
     * Gets the raw message.
     *
     * @return The raw message.
     */
    public byte[] getRawMessage() {
        return this.message;
    }

    /**
     * Gets the decrypted message.
     *
     * @return The decrypted message.
     */
    public byte[] getDecryptedMessage() {
        Cipher cipher = new Cipher(new KeyPair(this.senderPublicKey), this.recipientKeyPair);
        return cipher.decrypt(message);
    }
}
