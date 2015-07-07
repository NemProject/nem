package org.nem.core.model.ncc;

import org.nem.core.model.namespace.Namespace;
import org.nem.core.serialization.*;

/**
 * Pair containing a Namespace and a NamespaceMetaData
 */
public class NamespaceMetaDataPair implements SerializableEntity {
	private final Namespace namespace;
	private final NamespaceMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param namespace The namespace.
	 * @param metaData The meta data.
	 */
	public NamespaceMetaDataPair(final Namespace namespace, final NamespaceMetaData metaData) {
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
		this.metaData = deserializer.readObject("meta", NamespaceMetaData::new);
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
	public NamespaceMetaData getMetaData() {
		return this.metaData;
	}
}
