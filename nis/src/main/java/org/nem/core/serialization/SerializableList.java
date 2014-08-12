package org.nem.core.serialization;

import java.util.*;

/**
 * Helper class for storing lists of serializable entities
 */
public class SerializableList<T extends SerializableEntity> implements SerializableEntity {
	private static final String DEFAULT_LABEL = "data";

	private final List<T> list;
	private final String label;

	/**
	 * Creates a new list with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public SerializableList(final int initialCapacity) {
		this(initialCapacity, DEFAULT_LABEL);
	}

	/**
	 * Creates a new list with the specified capacity and custom label.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public SerializableList(final int initialCapacity, final String label) {
		this.list = new ArrayList<>(initialCapacity);
		this.label = label;
	}

	/**
	 * Creates a new list and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 */
	public SerializableList(final Collection<T> collection) {
		this(collection, DEFAULT_LABEL);
	}

	/**
	 * Creates a new list with a custom label and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 * @param label The name of the list
	 */
	public SerializableList(final Collection<T> collection, final String label) {
		this(collection.size(), label);
		collection.forEach(obj -> this.add(obj));
	}

	/**
	 * Deserializes a serializable list.
	 *
	 * @param deserializer The deserializer.
	 * @param elementDeserializer The element deserializer.
	 */
	public SerializableList(final Deserializer deserializer, final ObjectDeserializer<T> elementDeserializer) {
		this(deserializer, elementDeserializer, DEFAULT_LABEL);
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
		this.list = deserializer.readObjectArray(label, elementDeserializer);
		this.label = label;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObjectArray(this.label, this.list);
	}

	/**
	 * Adds a new item to this list.
	 *
	 * @param item The item.
	 */
	public final void add(final T item) {
		if (null == item) {
			throw new IllegalArgumentException("cannot add null item");
		}

		this.list.add(item);
	}

	/**
	 * Returns number of elements in this list.
	 *
	 * @return The number of elements.
	 */
	public int size() {
		return this.list.size();
	}

	/**
	 * Gets the label associated with this list.
	 *
	 * @return The label associated with this list.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Gets the element in this list at the specified index.
	 *
	 * @param i the index.
	 * @return The element.
	 */
	public T get(int i) {
		return this.list.get(i);
	}

	/**
	 * Compares this list with another list (that might have a different size)
	 * and finds the first non-equal element.
	 *
	 * @param rhs The other list.
	 * @return The index of the first difference.
	 */
	public int findFirstDifference(final SerializableList<T> rhs) {
		return findFirstDifferenceInternal(rhs);
	}

	private int findFirstDifferenceInternal(final SerializableList<?> rhs) {
		final int limit = Math.min(this.size(), rhs.size());

		for (int i = 0; i < limit; ++i) {
			if (!this.get(i).equals(rhs.get(i))) {
				return i;
			}
		}

		return limit;
	}

	/**
	 * Returns the underlying collection.
	 *
	 * @return The underlying collection.
	 */
	public Collection<T> asCollection() {
		return this.list;
	}

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SerializableList<?>)) {
			return false;
		}

		final SerializableList<?> rhs = (SerializableList<?>)obj;
		return this.size() == rhs.size() && this.size() == this.findFirstDifferenceInternal(rhs);
	}
}