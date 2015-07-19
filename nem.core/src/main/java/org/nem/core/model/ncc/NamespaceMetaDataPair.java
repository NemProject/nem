package org.nem.core.model.ncc;

import org.nem.core.model.namespace.Namespace;
import org.nem.core.serialization.*;

/**
 * Pair containing a namespace and meta data.
 */
public class NamespaceMetaDataPair implements SerializableEntity {
	private final Namespace namespace;
	private final DefaultMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param namespace The namespace.
	 * @param metaData The meta data.
	 */
	public NamespaceMetaDataPair(final Namespace namespace, final DefaultMetaData metaData) {
		this.namespace = namespace;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public NamespaceMetaDataPair(final Deserializer deserializer) {
		this.namespace = deserializer.readObject("namespace", Namespace::new);
		this.metaData = deserializer.readObject("meta", DefaultMetaData::new);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("namespace", this.namespace);
		serializer.writeObject("meta", this.metaData);
	}

	/**
	 * Gets the namespace.
	 *
	 * @return The namespace.
	 */
	public Namespace getNamespace() {
		return this.namespace;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public DefaultMetaData getMetaData() {
		return this.metaData;
	}
}
