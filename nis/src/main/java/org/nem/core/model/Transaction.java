package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction {

    public final int version;
    public final int type;
    public final Account sender;
    public Signature signature;
    public long fee;

    /**
     * Creates a new transaction.
     *
     * @param type The transaction type.
     * @param version The transaction version.
     * @param sender The transaction sender.
     */
    public Transaction(final int type, final int version, final Account sender) {
        this.type = type;
        this.version = version;
        this.sender = sender;

        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("sender must be owned to create transaction ");
    }

    /**
     * Deserializes a new transaction.
     *
     * @param type The transaction type.
     * @param deserializer The deserializer to use.
     */
    public Transaction(final int type, final ObjectDeserializer deserializer) {
        this.type = type;
        this.version = deserializer.readInt("version");
        this.sender = deserializer.readAccount("sender");
        this.fee = deserializer.readLong("fee");
        this.signature = deserializer.readSignature("signature");
    }

    //region Setters and Getters

    /**
     * Gets the version.
     *
     * @return The version.
     */
    public int getVersion() { return this.version; }

    /**
     * Gets the type.
     *
     * @return The type.
     */
    public int getType() { return this.type; }

    /**
     * Gets the sender.
     *
     * @return The sender.
     */
    public Account getSender() { return this.sender; }

    /**
     * Gets the signature.
     *
     * @return The signature.
     */
    public Signature getSignature() { return this.signature; }

    /**
     * Gets the fee.
     *
     * @return The fee.
     */
    public long getFee() { return Math.max(this.getMinimumFee(), this.fee); }

    /**
     * Sets the fee.
     *
     * @param fee The desired fee.
     */
    public void setFee(final long fee) { this.fee = fee; }

    //endregion

    /**
     * Serializes this object.
     *
     * @param serializer The serializer to use.
     */
    public void serialize(final ObjectSerializer serializer) {
        if (null == this.signature)
            throw new SerializationException("cannot serialize a transaction without a signature");

        this.serialize(serializer, true);
    }

    private void serialize(final ObjectSerializer serializer, boolean includeSignature) {
        serializer.writeInt("type", this.getType());
        serializer.writeInt("version", this.getVersion());
        serializer.writeAccount("sender", this.getSender());
        serializer.writeLong("fee", this.getFee());

        if (includeSignature)
            serializer.writeSignature("signature", this.signature);

        this.serializeImpl(serializer);
    }

    /**
     * Signs this transaction with the sender's private key.
     */
    public void sign() {
        if (!this.sender.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("cannot sign because sender is not self");

        // (1) serialize the entire transaction to a buffer
        byte[] transactionBytes = this.getBytes();

        // (2) sign the buffer
        Signer signer = new Signer(this.getSender().getKeyPair());
        this.signature = signer.sign(transactionBytes);
    }

    /**
     * Verifies that this transaction has been signed by the sender's public key.
     */
    public boolean verify() {
        if (null == this.signature)
            throw new CryptoException("cannot verify because signature does not exist");

        Signer signer = new Signer(this.getSender().getKeyPair());
        return signer.verify(this.getBytes(), this.signature);
    }

    /**
     * Determines if this transaction is valid.
     *
     * @return true if this transaction is valid.
     */
    public abstract boolean isValid();

    /**
     * Gets the minimum fee for this transaction.
     *
     * @return The minimum fee.
     */
    protected abstract long getMinimumFee();

    /**
     * Serializes derived-class state.
     *
     * @param serializer The serializer to use.
     */
    protected abstract void serializeImpl(final ObjectSerializer serializer);

    private byte[] getBytes() {
        try {
            try (BinarySerializer binarySerializer = new BinarySerializer()) {
                ObjectSerializer objectSerializer = new DelegatingObjectSerializer(binarySerializer);
                this.serialize(objectSerializer, false);
                return binarySerializer.getBytes();
            }
        }
        catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}