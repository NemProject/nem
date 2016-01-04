package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * Abstract base class for all meta data pairs.
 *
 * @param <TEntity> The entity type.
 * @param <TMetaData> The metadata type.
 */
public abstract class AbstractMetaDataPair<
		TEntity extends SerializableEntity,
		TMetaData extends SerializableEntity> implements SerializableEntity {
	private final TEntity entity;
	private final TMetaData metaData;
	private final String entityKey;
	private final String metaDataKey;

	/**
	 * Creates a new pair.
	 *
	 * @param entityKey The entity key.
	 * @param metaDataKey The meta data key.
	 * @param entity The entity.
	 * @param metaData The meta data.
	 */
	protected AbstractMetaDataPair(
			final String entityKey,
			final String metaDataKey,
			final TEntity entity,
			final TMetaData metaData) {
		this.entityKey = entityKey;
		this.metaDataKey = metaDataKey;
		this.entity = entity;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param entityKey The entity key.
	 * @param metaDataKey The meta data key.
	 * @param entityActivator The entity activator.
	 * @param metaDataActivator The meta data activator.
	 * @param deserializer The deserializer.
	 */
	protected AbstractMetaDataPair(
			final String entityKey,
			final String metaDataKey,
			final ObjectDeserializer<TEntity> entityActivator,
			final ObjectDeserializer<TMetaData> metaDataActivator,
			final Deserializer deserializer) {
		this.entityKey = entityKey;
		this.metaDataKey = metaDataKey;
		this.entity = deserializer.readObject(this.entityKey, entityActivator);
		this.metaData = deserializer.readObject(this.metaDataKey, metaDataActivator);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject(this.entityKey, this.entity);
		serializer.writeObject(this.metaDataKey, this.metaData);
	}

	/**
	 * Gets the entity.
	 *
	 * @return The entity.
	 */
	public TEntity getEntity() {
		return this.entity;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public TMetaData getMetaData() {
		return this.metaData;
	}
}
