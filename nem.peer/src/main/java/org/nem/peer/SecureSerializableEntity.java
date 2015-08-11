package org.nem.peer;

import org.nem.core.crypto.*;
import org.nem.core.model.HashUtils;
import org.nem.core.node.NodeIdentity;
import org.nem.core.serialization.*;
import org.nem.peer.node.ImpersonatingPeerException;

/**
 * Secure serializable entity that can be used to authenticate the sender.
 *
 * @param <T> The type of entity.
 */
public class SecureSerializableEntity<T extends SerializableEntity> implements SerializableEntity {

	private final T entity;
	private final Signature signature;
	private final NodeIdentity identity;

	/**
	 * Creates a secure entity.
	 *
	 * @param entity The entity.
	 * @param identity The identity.
	 */
	public SecureSerializableEntity(final T entity, final NodeIdentity identity) {
		this.entity = entity;
		final Hash hash = HashUtils.calculateHash(this.entity);
		this.signature = identity.sign(hash.getRaw());
		this.identity = identity;
	}

	/**
	 * Deserializes a secure entity.
	 *
	 * @param deserializer The deserializer.
	 * @param entityDeserializer The deserializer for the underlying entity.
	 */
	public SecureSerializableEntity(final Deserializer deserializer, final ObjectDeserializer<T> entityDeserializer) {
		this.entity = deserializer.readObject("entity", entityDeserializer);
		this.signature = Signature.readFrom(deserializer, "signature");
		this.identity = deserializer.readObject("identity", NodeIdentity::deserializeWithPublicKey);
	}

	/**
	 * Gets the entity.
	 *
	 * @return The entity.
	 */
	public T getEntity() {
		final Hash hash = HashUtils.calculateHash(this.entity);
		if (!this.identity.verify(hash.getRaw(), this.signature)) {
			throw new ImpersonatingPeerException("entity source cannot be verified");
		}

		return this.entity;
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
	 * Gets the identity.
	 *
	 * @return The identity.
	 */
	public NodeIdentity getIdentity() {
		return this.identity;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("entity", this.entity);
		Signature.writeTo(serializer, "signature", this.signature);
		serializer.writeObject("identity", this.identity);
	}
}
