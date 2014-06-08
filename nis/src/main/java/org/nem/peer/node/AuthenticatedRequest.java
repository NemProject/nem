package org.nem.peer.node;

import org.nem.core.serialization.*;

/**
 * Authenticated NIS request that can be used to authenticate the local node.
 *
 * @param <T> The type of entity.
 */
public class AuthenticatedRequest<T extends SerializableEntity> implements SerializableEntity {

	private final T entity;
	private final byte[] data;

	/**
	 * Creates an authenticated request.
	 *
	 * @param entity The entity.
	 * @param data The challenge data.
	 */
	public AuthenticatedRequest(final T entity, final byte[] data) {
		this.entity = entity;
		this.data = data;
	}

	/**
	 * Deserializes a request.
	 *
	 * @param deserializer The deserializer.
	 * @param entityDeserializer The entity deserializer.
	 */
	public AuthenticatedRequest(final Deserializer deserializer, final ObjectDeserializer<T> entityDeserializer) {
		this.entity = deserializer.readObject("entity", entityDeserializer);
		this.data = deserializer.readBytes("data");
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
	 * Gets the data.
	 *
	 * @return The data.
	 */
	public byte[] getData() {
		return this.data;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("entity", this.entity);
		serializer.writeBytes("data", this.data);
	}
}
