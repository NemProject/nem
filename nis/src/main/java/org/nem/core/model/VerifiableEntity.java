package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Base class for all entities that need to be verified
 * (e.g. blocks and transactions).
 */
public abstract class VerifiableEntity {

    private final int version;
    private final int type;
    private Account signer;
    private Signature signature;

    //region Constructors

    /**
     * Creates a new verifiable entity.
     *
     * @param type The entity type.
     * @param version The entity version.
     * @param signer The entity signer.
     */
    public VerifiableEntity(final int type, final int version, final Account signer) {
        if (!signer.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("signer private key is required to create a verifiable entity ");

        this.type = type;
        this.version = version;
        this.signer = signer;
    }

    /**
     * Deserializes a new transaction.
     *
     * @param type The transaction type.
     * @param deserializer The deserializer to use.
     */
    public VerifiableEntity(final int type, final ObjectDeserializer deserializer) {
        this.type = type;
        this.version = deserializer.readInt("version");
        this.signer = deserializer.readAccount("signer");
        this.signature = deserializer.readSignature("signature");
    }

    //endregion

    //region Getters and Setters

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
     * Gets the signer.
     *
     * @return The signer.
     */
    public Account getSigner() { return this.signer; }

    /**
     * Gets the signature.
     *
     * @return The signature.
     */
    public Signature getSignature() { return this.signature; }

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

    /**
     * Serializes this object.
     *
     * @param serializer The serializer to use.
     * @param includeSignature true if the serialization should include the signature.
     */
    private void serialize(final ObjectSerializer serializer, boolean includeSignature) {
        serializer.writeInt("type", this.getType());
        serializer.writeInt("version", this.getVersion());
        serializer.writeAccount("signer", this.getSigner());

        if (includeSignature)
            serializer.writeSignature("signature", this.getSignature());

        this.serializeImpl(serializer);
    }

    /**
     * Serializes derived-class state.
     *
     * @param serializer The serializer to use.
     */
    protected abstract void serializeImpl(final ObjectSerializer serializer);

    /**
     * Signs this entity with the owner's private key.
     */
    public void sign() {
        if (!this.signer.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("cannot sign because sender is not self");

        // (1) serialize the entire transaction to a buffer
        byte[] transactionBytes = this.getBytes();

        // (2) sign the buffer
        Signer signer = new Signer(this.signer.getKeyPair());
        this.signature = signer.sign(transactionBytes);
    }

    /**
     * Verifies that this transaction has been signed by the owner's public key.
     */
    public boolean verify() {
        if (null == this.signature)
            throw new CryptoException("cannot verify because signature does not exist");

        Signer signer = new Signer(this.signer.getKeyPair());
        return signer.verify(this.getBytes(), this.signature);
    }

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