package org.nem.core.serialization;

import java.util.*;

/**
 * Helper class for deserializing lists of serializable entities.
 * <br>
 * This class is tested indirectly by the tests for SerializableList.
 */
public class DeserializableList<T> {
	private static final String DEFAULT_LABEL = "data";

	protected final List<T> list;
	protected final String label;

	//region constructors

	/**
	 * Creates a new list with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	protected DeserializableList(final int initialCapacity) {
		this(initialCapacity, DEFAULT_LABEL);
	}

	/**
	 * Creates a new list with the specified capacity and custom label.
	 *
	 * @param initialCapacity The initial capacity.
	 * @param label The label for (de)serialization.
	 */
	protected DeserializableList(final int initialCapacity, final String label) {
		this.list = new ArrayList<>(initialCapacity);
		this.label = label;
	}

	/**
	 * Creates a new list and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 */
	protected DeserializableList(final Collection<T> collection) {
		this(collection, DEFAULT_LABEL);
	}

	/**
	 * Creates a new list with a custom label and initializes it with the elements in collection.
	 *
	 * @param collection The collection containing the initial elements.
	 * @param label The name of the list
	 */
	protected DeserializableList(final Collection<T> collection, final String label) {
		this(collection.size(), label);
		collection.forEach(this::add);
	}

	/**
	 * Deserializes a serializable list.
	 *
	 * @param deserializer The deserializer.
	 * @param elementDeserializer The element deserializer.
	 */
	public DeserializableList(final Deserializer deserializer, final ObjectDeserializer<T> elementDeserializer) {
		this(deserializer, elementDeserializer, DEFAULT_LABEL);
	}

	/**
	 * Deserializes a serializable list with a custom label.
	 *
	 * @param deserializer The deserializer.
	 * @param elementDeserializer The element deserializer.
	 * @param label The custom label.
	 */
	protected DeserializableList(
			final Deserializer deserializer,
			final ObjectDeserializer<T> elementDeserializer,
			final String label) {
		this.list = deserializer.readObjectArray(label, elementDeserializer);
		this.label = label;
	}

	//endregion

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
	public T get(final int i) {
		return this.list.get(i);
	}

	/**
	 * Compares this list with another list (that might have a different size)
	 * and finds the first non-equal element.
	 *
	 * @param rhs The other list.
	 * @return The index of the first difference.
	 */
	public int findFirstDifference(final DeserializableList<T> rhs) {
		return this.findFirstDifferenceInternal(rhs);
	}

	private int findFirstDifferenceInternal(final DeserializableList<?> rhs) {
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
		if (!(obj instanceof DeserializableList<?>)) {
			return false;
		}

		final DeserializableList<?> rhs = (DeserializableList<?>)obj;
		return this.size() == rhs.size() && this.size() == this.findFirstDifferenceInternal(rhs);
	}
}
