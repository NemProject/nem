package org.nem.nis.cache.delta;

import java.util.*;

/**
 * A skip list delta map.
 */
public class SkipListDeltaMap<TKey extends Comparable<?>, TValue> {
	private final DefaultSkipListMap<TKey, TValue> originalValues;
	private final DefaultSkipListMap<TKey, TValue> addedValues;
	private final DefaultSkipListMap<TKey, TValue> removedValues;

	/**
	 * Creates a new map.
	 */
	public SkipListDeltaMap() {
		this(new DefaultSkipListMap<>());
	}

	/**
	 * Creates a new map.
	 *
	 * @param originalValues The original values.
	 */
	SkipListDeltaMap(final DefaultSkipListMap<TKey, TValue> originalValues) {
		this.originalValues = originalValues;
		this.addedValues = new DefaultSkipListMap<>();
		this.removedValues = new DefaultSkipListMap<>();
	}

	/**
	 * Gets the size of the map.
	 *
	 * @return The size.
	 */
	public int size() {
		return this.originalValues.size() + this.addedValues.size() - this.removedValues.size();
	}

	/**
	 * Clears the delta map.
	 */
	public void clear() {
		this.removedValues.putAll(this.originalValues);
		this.addedValues.clear();
	}

	/**
	 * Gets a value indicating whether or not the key is contained in the map.
	 *
	 * @param key The key.
	 * @return true if the key is contained in the map, false otherwise.
	 */
	public boolean containsKey(final TKey key) {
		return !this.removedValues.containsKey(key) && (this.originalValues.containsKey(key) || this.addedValues.containsKey(key));
	}

	/**
	 * Gets a value indicating whether or not the key/values pair is contained in the map.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return true if the key/value pair is contained in the map, false otherwise.
	 */
	public boolean contains(final TKey key, final TValue value) {
		return !this.removedValues.contains(key, value)
				&& (this.originalValues.contains(key, value) || this.addedValues.contains(key, value));
	}

	/**
	 * Adds a key/value pair to the delta map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void put(final TKey key, final TValue value) {
		if (this.removedValues.contains(key, value)) {
			this.removedValues.remove(key, value);
			if (!this.originalValues.contains(key, value)) {
				this.addedValues.put(key, value);
			}

			return;
		}

		this.addedValues.put(key, value);
	}

	/**
	 * Removes a key/value pair from the delta map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void remove(final TKey key, final TValue value) {
		if (this.removedValues.contains(key, value)) {
			return;
		}

		if (this.originalValues.contains(key, value)) {
			this.removedValues.put(key, value);
			return;
		}

		if (this.addedValues.contains(key, value)) {
			this.addedValues.remove(key, value);
		}
	}

	/**
	 * Gets a collection of all values whose corresponding key is before the given key.
	 *
	 * @param key The key.
	 * @return The collections of values.
	 */
	public Collection<TValue> getValuesBefore(final TKey key) {
		final Collection<TValue> values = new ArrayList<>();
		values.addAll(this.originalValues.getValuesBefore(key));
		values.addAll(this.addedValues.getValuesBefore(key));
		values.removeAll(this.removedValues.getValuesBefore(key));
		return values;
	}

	/**
	 * Commits all changes to the "real" map.
	 */
	public void commit() {
		this.originalValues.putAll(this.addedValues);
		this.originalValues.removeAll(this.removedValues);

		this.addedValues.clear();
		this.removedValues.clear();
	}

	/**
	 * Shallow copies this delta map to another delta map.
	 *
	 * @param copy The other delta map.
	 */
	public void shallowCopyTo(final SkipListDeltaMap<TKey, TValue> copy) {
		this.shallowCopyTo(this.originalValues, copy.originalValues);
		this.shallowCopyTo(this.addedValues, copy.addedValues);
		this.shallowCopyTo(this.removedValues, copy.removedValues);
	}

	private void shallowCopyTo(final DefaultSkipListMap<TKey, TValue> source, final DefaultSkipListMap<TKey, TValue> dest) {
		dest.clear();
		dest.putAll(source);
	}

	public SkipListDeltaMap<TKey, TValue> rebase() {
		return new SkipListDeltaMap<>(this.originalValues);
	}

	public SkipListDeltaMap<TKey, TValue> deepCopy() {
		final SkipListDeltaMap<TKey, TValue> map = new SkipListDeltaMap<>();
		this.shallowCopyTo(map);
		return map;
	}
}
