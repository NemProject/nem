package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * Class for holding additional information about an object required by ncc.
 */
public class DefaultMetaData implements SerializableEntity {
	private final Long id;

	/**
	 * Creates a new meta data.
	 *
	 * @param id The entity id.
	 */
	public DefaultMetaData(final Long id) {
		this.id = id;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public DefaultMetaData(final Deserializer deserializer) {
		this.id = deserializer.readLong("id");
	}

	/**
	 * Returns the id of the object.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("id", this.id);
	}
}
