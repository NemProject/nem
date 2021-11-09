package org.nem.nis.cache.delta;

import java.util.*;
import java.util.stream.*;

/**
 * A delta map for storing mutable objects.
 *
 * @param <TKey> The key type.
 * @param <TValue> The value type.
 */
public class MutableObjectAwareDeltaMap<TKey, TValue extends Copyable<TValue>>
		implements
			DeltaMap<TKey, TValue>,
			CopyableDeltaMap<MutableObjectAwareDeltaMap<TKey, TValue>> {
	private final Map<TKey, TValue> originalValues;
	private final Map<TKey, TValue> copiedValues;
	private final Map<TKey, TValue> addedValues;
	private final Map<TKey, TValue> removedValues;
	private final boolean isMutable;

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
	private MutableObjectAwareDeltaMap(final Map<TKey, TValue> originalValues) {
		this(originalValues, false);
	}

	/**
	 * Creates a new mutable object aware delta map.
	 *
	 * @param originalValues The original values.
	 */
	private MutableObjectAwareDeltaMap(final Map<TKey, TValue> originalValues, final boolean isMutable) {
		this.originalValues = originalValues;
		this.copiedValues = new HashMap<>();
		this.addedValues = new HashMap<>();
		this.removedValues = new HashMap<>();
		this.isMutable = isMutable;
	}

	// region DeltaMap

	@Override
	public int size() {
		return this.originalValues.size() + this.addedValues.size() - this.removedValues.size();
	}

	@Override
	public void clear() {
		this.removedValues.putAll(this.originalValues);
		this.copiedValues.clear();
		this.addedValues.clear();
	}

	@Override
	public TValue get(final TKey key) {
		if (!this.isMutable) {
			return this.originalValues.getOrDefault(key, null);
		}

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

	@Override
	public TValue getOrDefault(final TKey key, final TValue defaultValue) {
		final TValue value = this.get(key);
		return null != value ? value : defaultValue;
	}

	@Override
	public void put(final TKey key, final TValue value) {
		if (!this.isMutable) {
			throw new IllegalStateException("put called on immutable MutableObjectAwareDeltaMap");
		}

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

	@Override
	public void remove(final TKey key) {
		if (!this.isMutable) {
			throw new IllegalStateException("put called on immutable MutableObjectAwareDeltaMap");
		}

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

	@Override
	public boolean containsKey(final TKey key) {
		return !this.removedValues.containsKey(key) && (this.originalValues.containsKey(key) || this.addedValues.containsKey(key));
	}

	@Override
	public Set<Map.Entry<TKey, TValue>> entrySet() {
		if (!this.isMutable) {
			throw new IllegalStateException("put called on immutable MutableObjectAwareDeltaMap");
		}

		final Map<TKey, TValue> map = new HashMap<>(this.size());
		this.originalValues.keySet().stream().filter(
				key -> !this.copiedValues.containsKey(key) && !this.addedValues.containsKey(key) && !this.removedValues.containsKey(key))
				.forEach(key -> this.copiedValues.put(key, this.originalValues.get(key).copy()));
		map.putAll(this.copiedValues);
		map.putAll(this.addedValues);
		return map.entrySet();
	}

	@Override
	public Stream<TValue> streamValues() {
		if (!this.isMutable) {
			throw new IllegalStateException("put called on immutable MutableObjectAwareDeltaMap");
		}

		this.originalValues.keySet().stream().filter(
				key -> !this.copiedValues.containsKey(key) && !this.addedValues.containsKey(key) && !this.removedValues.containsKey(key))
				.forEach(key -> this.copiedValues.put(key, this.originalValues.get(key).copy()));

		return Stream.concat(this.copiedValues.values().stream(), this.addedValues.values().stream());
	}

	// endregion

	// region CopyableDeltaMap

	@Override
	public void commit() {
		if (!this.isMutable) {
			throw new IllegalStateException("put called on immutable MutableObjectAwareDeltaMap");
		}

		this.originalValues.putAll(this.addedValues);
		this.originalValues.putAll(this.copiedValues);
		this.removedValues.keySet().forEach(this.originalValues::remove);

		this.copiedValues.clear();
		this.addedValues.clear();
		this.removedValues.clear();
	}

	@Override
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

	@Override
	public MutableObjectAwareDeltaMap<TKey, TValue> rebase() {
		return new MutableObjectAwareDeltaMap<>(this.originalValues, true);
	}

	@Override
	public MutableObjectAwareDeltaMap<TKey, TValue> deepCopy() {
		final MutableObjectAwareDeltaMap<TKey, TValue> map = new MutableObjectAwareDeltaMap<>(this.size());
		this.originalValues.forEach((key, value) -> map.originalValues.put(key, value.copy()));
		this.copiedValues.forEach((key, value) -> map.copiedValues.put(key, value.copy()));
		this.addedValues.forEach((key, value) -> map.addedValues.put(key, value.copy()));
		this.removedValues.forEach((key, value) -> map.removedValues.put(key, value.copy()));
		return map;
	}

	// endregion

	/**
	 * Gets the entry set of the delta map without copying. This should only be used by methods that return read only data.
	 *
	 * @return The entry set.
	 */
	public Set<Map.Entry<TKey, TValue>> readOnlyEntrySet() {
		final Set<Map.Entry<TKey, TValue>> entrySet = new HashSet<>();
		entrySet.addAll(this.copiedValues.entrySet());
		entrySet.addAll(this.addedValues.entrySet());
		entrySet.addAll(
				this.originalValues
						.entrySet().stream().filter(e -> !this.copiedValues.containsKey(e.getKey())
								&& !this.addedValues.containsKey(e.getKey()) && !this.removedValues.containsKey(e.getKey()))
						.collect(Collectors.toList()));
		return entrySet;
	}
}
