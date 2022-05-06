package org.nem.nis.cache.delta;

import java.util.*;
import java.util.stream.*;

/**
 * A delta map for storing immutable objects.
 *
 * @param <TKey> The key type.
 * @param <TValue> The value type.
 */
public class ImmutableObjectDeltaMap<TKey, TValue>
		implements
			DeltaMap<TKey, TValue>,
			CopyableDeltaMap<ImmutableObjectDeltaMap<TKey, TValue>> {
	private final Map<TKey, TValue> originalValues;
	private final Map<TKey, TValue> addedValues;
	private final Map<TKey, TValue> removedValues;

	/**
	 * Creates a new map.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public ImmutableObjectDeltaMap(final int initialCapacity) {
		this(new HashMap<>(initialCapacity));
	}

	/**
	 * Creates a map.
	 *
	 * @param originalValues The original values.
	 */
	public ImmutableObjectDeltaMap(final Map<TKey, TValue> originalValues) {
		this.originalValues = originalValues;
		this.addedValues = new HashMap<>();
		this.removedValues = new HashMap<>();
	}

	// region DeltaMap

	@Override
	public int size() {
		return this.originalValues.size() + this.addedValues.size() - this.removedValues.size();
	}

	@Override
	public void clear() {
		this.removedValues.putAll(this.originalValues);
		this.addedValues.clear();
	}

	@Override
	public TValue get(final TKey key) {
		if (this.removedValues.containsKey(key)) {
			return null;
		}

		final TValue value = this.originalValues.getOrDefault(key, null);
		return null != value ? value : this.addedValues.get(key);
	}

	@Override
	public TValue getOrDefault(final TKey key, final TValue defaultValue) {
		final TValue value = this.get(key);
		return null != value ? value : defaultValue;
	}

	@Override
	public void put(final TKey key, final TValue value) {
		if (this.removedValues.containsKey(key)) {
			this.removedValues.remove(key);

			// Need to add entry since value could have changed
			this.addedValues.put(key, value);
			return;
		}

		// TODO 20151007 BR -> *: what if the key/value pair exists in the original map?
		// > then size will return a wrong value.
		this.addedValues.put(key, value);
	}

	@Override
	public void remove(final TKey key) {
		if (this.removedValues.containsKey(key)) {
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
		final Set<Map.Entry<TKey, TValue>> entrySet = new HashSet<>();
		entrySet.addAll(this.addedValues.entrySet());
		entrySet.addAll(this.originalValues.entrySet().stream()
				.filter(e -> !this.addedValues.containsKey(e.getKey()) && !this.removedValues.containsKey(e.getKey()))
				.collect(Collectors.toList()));
		return entrySet;
	}

	@Override
	public Stream<TValue> streamValues() {
		return Stream.concat(this.originalValues.entrySet().stream()
				.filter(e -> !this.addedValues.containsKey(e.getKey()) && !this.removedValues.containsKey(e.getKey()))
				.map(Map.Entry::getValue), this.addedValues.values().stream());
	}

	// endregion

	// region CopyableDeltaMap

	@Override
	public void commit() {
		this.originalValues.putAll(this.addedValues);
		this.removedValues.keySet().forEach(this.originalValues::remove);

		this.addedValues.clear();
		this.removedValues.clear();
	}

	@Override
	public void shallowCopyTo(final ImmutableObjectDeltaMap<TKey, TValue> copy) {
		this.shallowCopyTo(this.originalValues, copy.originalValues);
		this.shallowCopyTo(this.addedValues, copy.addedValues);
		this.shallowCopyTo(this.removedValues, copy.removedValues);
	}

	private void shallowCopyTo(final Map<TKey, TValue> source, final Map<TKey, TValue> dest) {
		dest.clear();
		dest.putAll(source);
	}

	@Override
	public ImmutableObjectDeltaMap<TKey, TValue> rebase() {
		return new ImmutableObjectDeltaMap<>(this.originalValues);
	}

	@Override
	public ImmutableObjectDeltaMap<TKey, TValue> deepCopy() {
		// TODO 20151001 J-J: this is not really a deep copy because objects need to be copied too
		final ImmutableObjectDeltaMap<TKey, TValue> map = new ImmutableObjectDeltaMap<>(this.size());
		this.shallowCopyTo(map);
		return map;
	}

	// endregion
}
