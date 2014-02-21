package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public abstract class Transaction {

    public final int version;
    public final int type;
    public final Account sender;
    public Signature signature;
    public long fee;

    public int getVersion() { return this.version; }
    public int getType() { return this.type; }
    public Account getSender() { return this.sender; }
    public Signature getSignature() { return this.signature; }
    public long getFee() { return Math.max(this.getMinimumFee(), this.fee); }

    public void setFee(final long fee) { this.fee = fee; }

    public Transaction(final int type, final int version, final Account sender) {
        this.type = type;
        this.version = version;
        this.sender = sender;

        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("sender must be owned to create transaction ");
    }

    public Transaction(final int type, final ObjectDeserializer deserializer) throws Exception {
        this.type = type;
        this.version = deserializer.readInt("version");
        this.sender = deserializer.readAccount("sender");
        this.fee = deserializer.readLong("fee");
        this.signature = deserializer.readSignature("signature");
    }

    public void serialize(final ObjectSerializer serializer) throws Exception {
        if (null == this.signature)
            throw new SerializationException("cannot serialize a transaction without a signature");

        this.serialize(serializer, true);
    }

    private void serialize(final ObjectSerializer serializer, Boolean includeSignature) throws Exception {
        serializer.writeInt("type", this.getType());
        serializer.writeInt("version", this.getVersion());
        serializer.writeAccount("sender", this.getSender());
        serializer.writeLong("fee", this.getFee());

        if (includeSignature)
            serializer.writeSignature("signature", this.signature);

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

    public abstract boolean isValid();

    protected abstract long getMinimumFee();

    protected abstract void serializeImpl(final ObjectSerializer serializer) throws Exception;

    public byte[] getBytes() throws Exception {
        try (BinarySerializer binarySerializer = new BinarySerializer()) {
            ObjectSerializer objectSerializer = new DelegatingObjectSerializer(binarySerializer);
            this.serialize(objectSerializer, false);
            return binarySerializer.getBytes();
        }
    }
}