package org.nem.core.model;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * Helper class for storing lists of serializable entities
 */
public class SerializableList<T extends SerializableEntity> implements SerializableEntity {

	private final List<T> list;

	/**
	 * Creates a new list with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public SerializableList(int initialCapacity) {
		this.list = new ArrayList<>(initialCapacity);
	}

	/**
	 * Creates a new list and initializes it with the elements in list.
	 *
	 * @param list The list containing the initial elements.
	 */
	public SerializableList(final List<T> list) {
		this(list.size());
		for (final T item : list) {
			this.add(item);
		}
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObjectArray("data", this.list);
	}

	/**
	 * Adds a new item to this list.
	 *
	 * @param item The item.
	 */
	public final void add(final T item) {
		if (null == item)
			throw new IllegalArgumentException("cannot add null item");

		this.list.add(item);
	}

	/**
	 * Returns number of elements in this list.
	 *
	 * @return The number of elements.
	 */
	public int size() {
		return list.size();
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

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SerializableList<?>))
			return false;

		final SerializableList<?> rhs = (SerializableList<?>)obj;
		return this.size() == rhs.size() && this.size() == this.findFirstDifferenceInternal(rhs);
	}
}