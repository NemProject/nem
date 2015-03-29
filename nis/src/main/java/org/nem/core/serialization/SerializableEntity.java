package org.nem.core.serialization;

/**
 * An interface that should be implemented by classes that support serialization.
 */
public interface SerializableEntity {

	/**
	 * Serializes this entity.
	 *
	 * @param serializer The serializer to use.
	 */
	void serialize(final Serializer serializer);
}