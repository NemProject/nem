package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeltaMap<TKey, TValue> {
	private final Map<TKey, TValue> originalValues;
	private final Map<TKey, TValue> addedValues;
	private final Map<TKey, TValue> removedValues;

	public DeltaMap(final int initialCapacity) {
		this(new HashMap<>(initialCapacity));
	}

	public DeltaMap(final Map<TKey, TValue> originalValues) {
		this.originalValues = originalValues;
		this.addedValues = new HashMap<>();
		this.removedValues = new HashMap<>();
	}

	public int size() {
		return this.originalValues.size() + this.addedValues.size() - this.removedValues.size();
	}

	public void clear() {
		this.removedValues.putAll(this.originalValues);
		this.addedValues.clear();
	}

	public TValue get(final TKey key) {
		if (this.removedValues.containsKey(key)) {
			return null;
		}

		final TValue value = this.originalValues.getOrDefault(key, null);
		return null != value ? value : this.addedValues.get(key);
	}

	public void put(final TKey key, final TValue value) {
		if (this.removedValues.containsKey(key)) {
			this.removedValues.remove(key);
			if (!this.originalValues.containsKey(key)) {
				this.addedValues.put(key, value);
			}

			return;
		}

		// TODO 20151001 J-*: this check might not be appropriate in this class if different caches have different behaviors
		if (null != this.get(key)) {
			throw new IllegalArgumentException(String.format("key %s already exists in cache", key));
		}

		this.addedValues.put(key, value);
	}

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

	public boolean containsKey(final TKey key) {
		return !this.removedValues.containsKey(key) && (this.originalValues.containsKey(key) || this.addedValues.containsKey(key));
	}

	public void shallowCopyTo(final DeltaMap<TKey, TValue> copy) {
		this.shallowCopyTo(this.originalValues, copy.originalValues);
		this.shallowCopyTo(this.addedValues, copy.addedValues);
		this.shallowCopyTo(this.removedValues, copy.removedValues);
	}

	private void shallowCopyTo(final Map<TKey, TValue> source, final Map<TKey, TValue> dest) {
		dest.clear();
		dest.putAll(source);
	}

	public void commit() {
		this.originalValues.putAll(this.addedValues);
		this.removedValues.keySet().forEach(this.originalValues::remove);

		this.addedValues.clear();
		this.removedValues.clear();
	}

	public DeltaMap<TKey, TValue> rebase() {
		return new DeltaMap<>(this.originalValues);
	}

	public DeltaMap<TKey, TValue> deepCopy() {
		// TODO 20151001 J-J: this is not really a deep copy because objects need to be copied too
		final DeltaMap<TKey, TValue> map = new DeltaMap<>(this.size());
		this.shallowCopyTo(map);
		return map;
	}

	public Set<Map.Entry<TKey, TValue>> entrySet() {
		final Set<Map.Entry<TKey, TValue>> entrySet = new HashSet<>();
		entrySet.addAll(this.addedValues.entrySet());
		entrySet.addAll(this.originalValues.entrySet().stream()
				.filter(e -> !this.addedValues.containsKey(e.getKey()) && !this.removedValues.containsKey(e.getKey()))
				.collect(Collectors.toList()));
		return entrySet;
	}
}