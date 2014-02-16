package org.nem.core.model;

import org.nem.core.crypto.*;
import java.util.*;

public class Account {

    private final KeyPair keyPair;
    private final String id;
    private final List<byte[]> messages;
    private long balance;
    private String label;

    public Account(final KeyPair keyPair) throws Exception {
        this.keyPair = keyPair;
        this.id = Address.fromPublicKey(keyPair.getPublicKey());
        this.messages = new ArrayList<>();
        this.balance = 0;
    }

    public byte[] getPublicKey() { return this.keyPair.getPublicKey(); }

    public String getId() { return this.id; }

    public long getBalance() { return this.balance; }
    public void incrementBalance(final long balance) { this.balance += balance; }

    public String getLabel() { return this.label; }
    public void setLabel(final String label) { this.label = label; }

     // TODO: the messaging API still needs to be cleaned up a little bit

    public List<byte[]> getMessages() { return this.messages; }
    public void addMessage(final Account sender, byte[] message) {
        Cipher cipher = new Cipher(new KeyPair(sender.getPublicKey()), this.keyPair);
        byte[] decodedMessage = cipher.decrypt(message);
        this.messages.add(decodedMessage);
    }
}