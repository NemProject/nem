package org.nem.core.model;

import org.nem.core.crypto.*;
import java.util.*;

/**
 * A NEM account.
 */
public class Account {

    private final KeyPair keyPair;
    private final Address address;
    private final List<Message> messages;
    private long balance;
    private String label;

    public Account(final KeyPair keyPair) {
        this.keyPair = keyPair;
        this.address = Address.fromPublicKey(keyPair.getPublicKey());
        this.messages = new ArrayList<>();
        this.balance = 0;
    }

    public KeyPair getKeyPair() { return this.keyPair; }

    public byte[] getPublicKey() { return this.keyPair.getPublicKey(); }

    public Address getAddress() { return this.address; }

    public long getBalance() { return this.balance; }
    public void incrementBalance(final long balance) { this.balance += balance; }

    public String getLabel() { return this.label; }
    public void setLabel(final String label) { this.label = label; }

     // TODO: the messaging API still needs to be cleaned up a little bit

    public List<Message> getMessages() { return this.messages; }
    public void addMessage(final Account sender, byte[] message) {
        this.messages.add(new Message(this.keyPair, sender, message));
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Account))
            return false;

        Account rhs = (Account)obj;
        return this.address.equals(rhs.address);
    }

    /**
     * A message sent from one account to another.
     */
    public class Message {
        private final KeyPair recipientKeyPair;
        private final byte[] senderPublicKey;
        private final byte[] message;

        public Message(final KeyPair recipientKeyPair, final Account sender, byte[] message) {
            this.recipientKeyPair = recipientKeyPair;
            this.senderPublicKey = sender.getPublicKey();
            this.message = message;
        }

        /**
         * Gets the raw encrypted message.
         *
         * @return The raw encrypted message.
         */
        public byte[] getEncryptedMessage() {
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
}