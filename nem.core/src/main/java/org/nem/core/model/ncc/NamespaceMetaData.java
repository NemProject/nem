package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * Class for holding additional information about a namespace required by ncc.
 */
public class NamespaceMetaData implements SerializableEntity {
	private final Long id;

	/**
	 * Creates a new meta data.
	 *
	 * @param id The namespace id.
	 */
	public NamespaceMetaData(final Long id) {
		this.id = id;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public NamespaceMetaData(final Deserializer deserializer) {
		this.id = deserializer.readLong("id");
	}

	/**
	 * Returns the id of a namespace.
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
