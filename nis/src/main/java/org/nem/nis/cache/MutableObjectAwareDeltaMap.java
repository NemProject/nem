package org.nem.nis.cache;

import java.util.*;

/**
 * Delta map that returns mutable TValue objects.
 */
public class MutableObjectAwareDeltaMap<TKey, TValue extends CopyableCache<TValue>> {
	private final Map<TKey, TValue> originalValues;
	private final Map<TKey, TValue> copiedValues;
	private final Map<TKey, TValue> addedValues;
	private final Map<TKey, TValue> removedValues;

	/**
	 * Creates a new mutable object aware delta map.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public MutableObjectAwareDeltaMap(final int initialCapacity) {
		this(new HashMap<>(initialCapacity));
	}

	/**
	 * Creates a new mutable object aware delta map.
	 *
	 * @param originalValues The original values.
	 */
	public MutableObjectAwareDeltaMap(final Map<TKey, TValue> originalValues) {
		this.originalValues = originalValues;
		this.copiedValues = new HashMap<>();
		this.addedValues = new HashMap<>();
		this.removedValues = new HashMap<>();
	}

	/**
	 * Gets the size of the delta map.
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
	 * Gets a value from the map.
	 * The implementation returns a copy of the value if it is found in the original map and not already copied.
	 *
	 * @param key The key.
	 * @return The value.
	 */
	public TValue get(final TKey key) {
		if (this.removedValues.containsKey(key)) {
			return null;
		}

		final TValue copiedValue = this.copiedValues.getOrDefault(key, null);
		if (null != copiedValue) {
			return copiedValue;
		}

		final TValue originalValue = this.originalValues.getOrDefault(key, null);
		if (null != originalValue) {
			final TValue copy = originalValue.copy();
			this.copiedValues.put(key, copy);
			return copy;
		}

		return this.addedValues.getOrDefault(key, null);
	}

	/**
	 * Gets a value from the map or default to the given default value if the key is unknown.
	 *
	 * @param key The key.
	 * @param defaultValue The default value.
	 * @return The value.
	 */
	public TValue getOrDefault(final TKey key, final TValue defaultValue) {
		final TValue value = this.get(key);
		return null != value ? value : defaultValue;
	}

	/**
	 * Adds a key/value pair to the delta map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void put(final TKey key, final TValue value) {
		if (this.removedValues.containsKey(key)) {
			this.removedValues.remove(key);
		}

		if (!this.originalValues.containsKey(key)) {
			this.addedValues.put(key, value);
		} else {
			// note: this might be a value that was obtained via get() and changed afterwards
			this.copiedValues.put(key, value);
		}
	}

	/**
	 * Removes a key/value pair from the delta map.
	 *
	 * @param key The key
	 */
	public void remove(final TKey key) {
		if (this.removedValues.containsKey(key)) {
			return;
		}

		final TValue copiedValue = this.copiedValues.getOrDefault(key, null);
		if (null != copiedValue) {
			this.copiedValues.remove(key);
			this.removedValues.put(key, copiedValue);
			return;
		}

		final TValue value = this.originalValues.getOrDefault(key, null);
		if (null != value) {
			this.removedValues.put(key, value);
			return;
		}

		if (this.addedValues.containsKey(key)) {
			this.addedValues.remove(key);
		}
	}

	/**
	 * Gets a value indications whether or not the delta map contains the key.
	 *
	 * @param key The key.
	 * @return true if the delta map contains the key, false otherwise.
	 */
	public boolean containsKey(final TKey key) {
		return !this.removedValues.containsKey(key) && (this.originalValues.containsKey(key) || this.addedValues.containsKey(key));
	}

	/**
	 * Shallow copies this delta map to the given delta map.
	 *
	 * @param copy The delta map to copy to.
	 */
	public void shallowCopyTo(final MutableObjectAwareDeltaMap<TKey, TValue> copy) {
		this.shallowCopyTo(this.originalValues, copy.originalValues);
		this.shallowCopyTo(this.copiedValues, copy.copiedValues);
		this.shallowCopyTo(this.addedValues, copy.addedValues);
		this.shallowCopyTo(this.removedValues, copy.removedValues);
	}

	private void shallowCopyTo(final Map<TKey, TValue> source, final Map<TKey, TValue> dest) {
		dest.clear();
		dest.putAll(source);
	}

	/**
	 * Commits all changes to the original delta map.
	 */
	public void commit() {
		this.originalValues.putAll(this.addedValues);
		this.originalValues.putAll(this.copiedValues);
		this.removedValues.keySet().forEach(this.originalValues::remove);

		this.copiedValues.clear();
		this.addedValues.clear();
		this.removedValues.clear();
	}

	/**
	 * Rebases this delta map.
	 *
	 * @return The rebased delta map.
	 */
	public MutableObjectAwareDeltaMap<TKey, TValue> rebase() {
		return new MutableObjectAwareDeltaMap<>(this.originalValues);
	}

	/**
	 * Gets a deep copy of this delta map.
	 *
	 * @return The deep copy.
	 */
	public MutableObjectAwareDeltaMap<TKey, TValue> deepCopy() {
		final MutableObjectAwareDeltaMap<TKey, TValue> map = new MutableObjectAwareDeltaMap<>(this.size());
		this.originalValues.forEach((key, value) -> map.originalValues.put(key, value.copy()));
		this.copiedValues.forEach((key, value) -> map.copiedValues.put(key, value.copy()));
		this.addedValues.forEach((key, value) -> map.addedValues.put(key, value.copy()));
		this.removedValues.forEach((key, value) -> map.removedValues.put(key, value.copy()));
		return map;
	}
}
