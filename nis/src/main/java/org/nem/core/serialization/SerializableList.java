package org.nem.core.serialization;

import java.util.Collection;

/**
 * Helper class for storing lists of serializable entities
 */
public class SerializableList<T extends SerializableEntity> extends DeserializableList<T> implements SerializableEntity {

	//region constructors

	/**
	 * Creates a new list with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public SerializableList(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new list with the specified capacity and custom label.
	 *
	 * @param initialCapacity The initial capacity.
	 * @param label The label for (de)serialization.
	 */
	public SerializableList(final int initialCapacity, final String label) {
		super(initialCapacity, label);
	}

	/**
	 * Creates a new list and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 */
	public SerializableList(final Collection<T> collection) {
		super(collection);
	}

	/**
	 * Creates a new list with a custom label and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 * @param label The name of the list
	 */
	public SerializableList(final Collection<T> collection, final String label) {
		super(collection, label);
	}

	/**
	 * Deserializes a serializable list.
	 *
	 * @param deserializer The deserializer.
	 * @param elementDeserializer The element deserializer.
	 */
	public SerializableList(final Deserializer deserializer, final ObjectDeserializer<T> elementDeserializer) {
		super(deserializer, elementDeserializer);
	}

	/**
	 * Deserializes a serializable list with a custom label.
	 *
	 * @param deserializer The deserializer.
	 * @param elementDeserializer The element deserializer.
	 * @param label The custom label.
	 */
	public SerializableList(
			final Deserializer deserializer,
			final ObjectDeserializer<T> elementDeserializer,
			final String label) {
		super(deserializer, elementDeserializer, label);
	}

	//endregion

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObjectArray(this.label, this.list);
	}
}