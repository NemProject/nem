package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

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
	private final Account signer;
	private final TimeInstant timestamp;
	private Signature signature;

	//region Constructors

	/**
	 * Creates a new verifiable entity.
	 *
	 * @param type The entity type.
	 * @param version The entity version.
	 * @param timestamp The entity timestamp.
	 * @param signer The entity signer.
	 */
	public VerifiableEntity(final int type, final int version, final TimeInstant timestamp, final Account signer) {
		if (null == signer.getKeyPair()) {
			throw new IllegalArgumentException("signer key pair is required to create a verifiable entity ");
		}

		this.type = type;
		this.version = version;
		this.timestamp = timestamp;
		this.signer = signer;
	}

	/**
	 * Deserializes a new transaction.
	 *
	 * @param type The transaction type.
	 * @param options Deserialization options.
	 * @param deserializer The deserializer to use.
	 */
	public VerifiableEntity(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		this.type = type;
		this.version = deserializer.readInt("version");
		this.timestamp = TimeInstant.readFrom(deserializer, "timestamp");
		this.signer = Account.readFrom(deserializer, "signer", AddressEncoding.PUBLIC_KEY);

		if (DeserializationOptions.VERIFIABLE == options) {
			this.signature = Signature.readFrom(deserializer, "signature");
		}
	}

	//endregion

	//region Getters and Setters

	/**
	 * Gets the version.
	 *
	 * @return The version.
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * Gets the type.
	 *
	 * @return The type.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Gets the signer.
	 *
	 * @return The signer.
	 */
	public Account getSigner() {
		return this.signer;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timestamp;
	}

	/**
	 * Gets the signature.
	 *
	 * @return The signature.
	 */
	public Signature getSignature() {
		return this.signature;
	}

	/**
	 * Sets the signature.
	 *
	 * @param signature The signature.
	 */
	public void setSignature(final Signature signature) {
		this.signature = signature;
	}

	//endregion

	@Override
	public void serialize(final Serializer serializer) {
		if (null == this.signature) {
			throw new SerializationException("cannot serialize a entity without a signature");
		}

		this.serialize(serializer, true);
	}

	private void serialize(final Serializer serializer, final boolean includeSignature) {
		serializer.writeInt("type", this.getType());
		serializer.writeInt("version", this.getVersion());
		TimeInstant.writeTo(serializer, "timestamp", this.getTimeStamp());
		Account.writeTo(serializer, "signer", this.getSigner(), AddressEncoding.PUBLIC_KEY);

		if (includeSignature) {
			Signature.writeTo(serializer, "signature", this.getSignature());
		}

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
		this.signBy(this.signer);
	}

	/**
	 * Signs this entity using private key of specified account.
	 *
	 * @param account The account to use for signing.
	 */
	public void signBy(final Account account) {
		if (!account.getKeyPair().hasPrivateKey()) {
			throw new IllegalArgumentException("cannot sign because signer does not have private key");
		}

		// (1) serialize the entire transaction to a buffer
		final byte[] transactionBytes = this.getBytes();

		// (2) sign the buffer
		final Signer signer = new Signer(account.getKeyPair());
		this.signature = signer.sign(transactionBytes);
	}

	/**
	 * Verifies that this transaction has been signed by the owner's public key.
	 *
	 * @return True if verification succeeded, false otherwise.
	 */
	public boolean verify() {
		if (null == this.signature) {
			throw new CryptoException("cannot verify because signature does not exist");
		}

		final Signer signer = new Signer(this.signer.getKeyPair());
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
		return serializer -> this.serialize(serializer, false);
	}
}