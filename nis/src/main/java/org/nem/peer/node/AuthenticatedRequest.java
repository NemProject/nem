package org.nem.peer.node;

import org.nem.core.serialization.*;

/**
 * Authenticated NIS request that can be used to authenticate the local node.
 *
 * @param <T> The type of entity.
 */
public class AuthenticatedRequest<T extends SerializableEntity> implements SerializableEntity {

	private final T entity;
	private final NodeChallenge challenge;

	/**
	 * Creates an authenticated request.
	 *
	 * @param entity The entity.
	 * @param challenge The challenge data.
	 */
	public AuthenticatedRequest(final T entity, final NodeChallenge challenge) {
		this.entity = entity;
		this.challenge = challenge;
	}

	/**
	 * Deserializes a request.
	 *
	 * @param deserializer The deserializer.
	 * @param entityDeserializer The entity deserializer.
	 */
	public AuthenticatedRequest(final Deserializer deserializer, final ObjectDeserializer<T> entityDeserializer) {
		this.entity = deserializer.readObject("entity", entityDeserializer);
		this.challenge = deserializer.readObject("challenge", NodeChallenge::new);
	}

	/**
	 * Gets the entity.
	 *
	 * @return The entity.
	 */
	public T getEntity() {
		return this.entity;
	}

	/**
	 * Gets the challenge.
	 *
	 * @return The challenge.
	 */
	public NodeChallenge getChallenge() {
		return this.challenge;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("entity", this.entity);
		serializer.writeObject("challenge", this.challenge);
	}
}
