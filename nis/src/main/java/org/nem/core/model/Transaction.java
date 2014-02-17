package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public abstract class Transaction {

    public final int version;
    public final int type;
    public final Account sender;
    private final Signer signer;
    public Signature signature;

    public int getVersion() { return this.version; }
    public int getType() { return this.type; }
    public Account getSender() { return this.sender; }
    public Signature getSignature() { return this.signature; }

    public Transaction(final int version, final int type, final Account sender) {
        this.version = version;
        this.type = type;
        this.sender = sender;
        this.signer = new Signer(this.getSender().getKeyPair());

        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("sender must be owned to create transaction ");
    }

    public Transaction(final Deserializer deserializer) throws Exception {
        this.signature = deserializer.readSignature();
        this.version = deserializer.readInt();
        this.type = deserializer.readInt();
        this.sender = deserializer.readAccount();
        this.signer = new Signer(this.getSender().getKeyPair());
    }

    public void serialize(final Serializer serializer) throws Exception {
        if (null == this.signature)
            throw new SerializationException("cannot serialize a transaction without a signature");

        this.serialize(serializer, true);
    }

    private void serialize(final Serializer serializer, Boolean includeSignature) throws Exception {
        if (includeSignature)
            serializer.writeSignature(this.signature);

        serializer.writeInt(this.getVersion());
        serializer.writeInt(this.getType());
        serializer.writeAccount(this.getSender());
        this.serializeImpl(serializer);
    }

    public void sign() throws Exception {
        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("cannot sign because sender is not self");

        // (1) serialize the entire transaction to a buffer
        byte[] transactionBytes = this.getBytes();

        // (2) sign the buffer
        Signer signer = new Signer(this.getSender().getKeyPair());
        this.signature = signer.sign(transactionBytes);
    }

    public Boolean verify() throws Exception {
        return signer.verify(this.getBytes(), this.signature);
    }

    protected abstract void serializeImpl(final Serializer serializer) throws Exception;

    public byte[] getBytes() throws Exception {
        try (BinarySerializer binarySerializer = new BinarySerializer()) {
            this.serialize(binarySerializer, false);
            return binarySerializer.getBytes();
        }
    }
}