package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public abstract class Transaction {

    public final int version;
    public final int type;
    public final Account sender;
    public Signature signature;

    public int getVersion() { return this.version; }
    public int getType() { return this.type; }
    public Account getSender() { return this.sender; }
    public Signature getSignature() { return this.signature; }

    public Transaction(final int version, final int type, final Account sender) {
        this.version = version;
        this.type = type;
        this.sender = sender;

        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("sender must be owned to create transaction ");
    }

    public Transaction(final ObjectDeserializer deserializer) throws Exception {
        this.signature = deserializer.readSignature("signature");
        this.version = deserializer.readInt("version");
        this.type = deserializer.readInt("type");
        this.sender = deserializer.readAccount("sender");
    }

    public void serialize(final ObjectSerializer serializer) throws Exception {
        if (null == this.signature)
            throw new SerializationException("cannot serialize a transaction without a signature");

        this.serialize(serializer, true);
    }

    private void serialize(final ObjectSerializer serializer, Boolean includeSignature) throws Exception {
        if (includeSignature)
            serializer.writeSignature("signature", this.signature);

        serializer.writeInt("version", this.getVersion());
        serializer.writeInt("type", this.getType());
        serializer.writeAccount("sender", this.getSender());
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
        Signer signer = new Signer(this.getSender().getKeyPair());
        return signer.verify(this.getBytes(), this.signature);
    }

    protected abstract void serializeImpl(final ObjectSerializer serializer) throws Exception;

    public byte[] getBytes() throws Exception {
        try (BinarySerializer binarySerializer = new BinarySerializer()) {
            ObjectSerializer objectSerializer = new DelegatingObjectSerializer(binarySerializer);
            this.serialize(objectSerializer, false);
            return binarySerializer.getBytes();
        }
    }
}