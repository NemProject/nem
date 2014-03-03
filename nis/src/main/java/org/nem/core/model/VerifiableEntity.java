package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Base class for all entities that need to be verified
 * (e.g. blocks and transactions).
 */
public abstract class VerifiableEntity implements SerializableEntity {

    /**
     * Enumeration of deserialization options.
     */
    public enum DeserializationOptions {
        /**
         * The serialized data includes a signature and is verifiable.
         */
        VERIFIABLE,

        /**
         * The serialized data does not include a signature and is not verifiable.
         */
        NON_VERIFIABLE
    }

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
        this.type = type;
        this.version = version;
        this.signer = signer;
    }

    /**
     * Deserializes a new transaction.
     *
     * @param type The transaction type.
     * @param options Deserialization options.
     * @param deserializer The deserializer to use.
     */
    public VerifiableEntity(final int type, DeserializationOptions options,  Deserializer deserializer) {
        this.type = type;
        this.version = deserializer.readInt("version");
        this.signer = SerializationUtils.readAccount(deserializer, "signer");

        if (DeserializationOptions.VERIFIABLE == options)
            this.signature = SerializationUtils.readSignature(deserializer, "signature");
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


	/**
	 * Sets the signature.
	 *
	 * @param signature The signature.
	 */
	public void setSignature(Signature signature) {
		this.signature = signature;
	}

    //endregion

    @Override
    public void serialize(final Serializer serializer) {
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
    private void serialize(final Serializer serializer, boolean includeSignature) {
		serializer.writeInt("type", this.getType());

		if (includeSignature)
			SerializationUtils.writeSignature(serializer, "signature", this.getSignature());

		serializer.writeInt("version", this.getVersion());
        SerializationUtils.writeAccount(serializer, "signer", this.getSigner());

        this.serializeImpl(serializer);
    }

    /**
     * Serializes derived-class state.
     *
     * @param serializer The serializer to use.
     */
    protected abstract void serializeImpl(final Serializer serializer);

    /**
     * Signs this entity with the owner's private key.
     */
    public void sign() {
        if (this.signer.getKeyPair() == null)
			throw new InvalidParameterException("cannot sign, missing key");

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

		if (this.signer.getKeyPair() == null)
			throw new InvalidParameterException("cannot verify, missing key");

		Signer signer = new Signer(this.signer.getKeyPair());
        return signer.verify(this.getBytes(), this.signature);
    }

    private byte[] getBytes() {
        return BinarySerializer.serializeToBytes(this.asNonVerifiable());
    }

    /**
     * Returns a non-verifiable serializer for the current entity.
     *
     * @return A non-verifiable serializer.
     */
    public SerializableEntity asNonVerifiable() {
        return new NonVerifiableSerializationAdapter(this);
    }

    /**
     * A serialization adapter for VerifiableEntity that serializes the entity
     * without a signature.
     */
    public static class NonVerifiableSerializationAdapter implements SerializableEntity {

        final VerifiableEntity entity;

        /**
         * Creates a non-verifiable serialization adapter for entity.
         *
         * @param entity The entity.
         */
        public NonVerifiableSerializationAdapter(final VerifiableEntity entity) {
            this.entity = entity;
        }

        @Override
        public void serialize(Serializer serializer) {
            entity.serialize(serializer, false);
        }
    }

}