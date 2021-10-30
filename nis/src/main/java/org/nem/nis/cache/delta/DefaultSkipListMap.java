package org.nem.nis.cache.delta;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * A default skip list map implementation.
 */
public class DefaultSkipListMap<TKey extends Comparable<?>, TValue> {
	private final ConcurrentSkipListMap<TKey, Set<TValue>> map;

	/**
	 * Creates a new map.
	 */
	public DefaultSkipListMap() {
		this(new ConcurrentSkipListMap<>());
	}

	/**
	 * Creates a new map.
	 *
	 * @param map The original map.
	 */
	public DefaultSkipListMap(final ConcurrentSkipListMap<TKey, Set<TValue>> map) {
		this.map = map;
	}

	/**
	 * Gets the size of the map.
	 *
	 * @return The size.
	 */
	public int size() {
		return this.map.isEmpty()
				? 0
				: this.map.keySet().stream().mapToInt(key -> this.map.get(key).size()).reduce(Integer::sum).getAsInt();
	}

	/**
	 * Clears the map.
	 */
	public void clear() {
		this.map.clear();
	}

	// TODO 20151124 J-B: should probably add test for this
	/**
	 * Gets a value indicating whether or not the key is contained in the map.
	 *
	 * @param key The key.
	 * @return true if the key is contained in the map, false otherwise.
	 */
	public boolean containsKey(final TKey key) {
		return this.map.containsKey(key);
	}

	/**
	 * Gets a value indicating whether or not the key/values pair is contained in the map.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return true if the key/value pair is contained in the map, false otherwise.
	 */
	public boolean contains(final TKey key, final TValue value) {
		final Set<TValue> values = this.map.getOrDefault(key, new HashSet<>());
		return values.contains(value);
	}

	/**
	 * Adds a key/value pair to the map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void put(final TKey key, final TValue value) {
		final Set<TValue> values = this.map.getOrDefault(key, new HashSet<>());
		this.map.put(key, values);
		values.add(value);
	}

	/**
	 * Adds a collection of key/value pair to the map.
	 *
	 * @param key The key.
	 * @param newValues The collection of new values.
	 */
	public void put(final TKey key, final Collection<TValue> newValues) {
		final Set<TValue> values = this.map.getOrDefault(key, new HashSet<>());
		this.map.put(key, values);
		values.addAll(newValues);
	}

	/**
	 * Adds key/value pairs contained in a map to this map.
	 *
	 * @param map The map.
	 */
	public void putAll(final DefaultSkipListMap<TKey, TValue> map) {
		map.entrySet().forEach(e -> this.put(e.getKey(), e.getValue()));
	}

	/**
	 * Removes a key/value pair from the map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	public void remove(final TKey key, final TValue value) {
		final Set<TValue> values = this.map.getOrDefault(key, new HashSet<>());
		values.remove(value);
		// TODO 20151128 J-J-B: not sure if there's a way to test this without exposing the underlying map size
		if (values.isEmpty()) {
			this.map.remove(key);
		}
	}

	/**
	 * Removes all key/value pairs contained in the map from this map.
	 *
	 * @param map The map.
	 */
	public void removeAll(final DefaultSkipListMap<TKey, TValue> map) {
		map.entrySet().forEach(e -> {
			final TKey key = e.getKey();
			final Set<TValue> values = e.getValue();
			values.forEach(value -> this.remove(key, value));
		});
	}

	/**
	 * Gets a collection of all values whose corresponding key is before the given key.
	 *
	 * @param key The key.
	 * @return The collections of values.
	 */
	public Collection<TValue> getValuesBefore(final TKey key) {
		return this.map.headMap(key).values().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	/**
	 * Gets the entry set for this map.
	 *
	 * @return The entry set.
	 */
	public Set<Map.Entry<TKey, Set<TValue>>> entrySet() {
		return this.map.entrySet();
	}
}
