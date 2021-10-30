package org.nem.nis.cache.delta;

import java.util.*;
import java.util.stream.Stream;

/**
 * A special map implementation that tracks pending changes inbetween commits.
 *
 * @param <TKey> The key type.
 * @param <TValue> The value type.
 */
@SuppressWarnings("unused")
public interface DeltaMap<TKey, TValue> {

	/**
	 * Gets the size of the map.
	 *
	 * @return The size.
	 */
	int size();

	/**
	 * Clears the map.
	 */
	void clear();

	/**
	 * Gets a value from the map.
	 *
	 * @param key The key.
	 * @return The value.
	 */
	TValue get(final TKey key);

	/**
	 * Gets a value from the map or the given default value if the key is unknown.
	 *
	 * @param key The key.
	 * @param defaultValue The default value.
	 * @return The value.
	 */
	TValue getOrDefault(final TKey key, final TValue defaultValue);

	/**
	 * Adds a key/value pair to the delta map.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	void put(final TKey key, final TValue value);

	/**
	 * Removes a key/value pair from the delta map.
	 *
	 * @param key The key
	 */
	void remove(final TKey key);

	/**
	 * Gets a value indicating whether or not the map contains the key.
	 *
	 * @param key The key.
	 * @return true if the delta map contains the key, false otherwise.
	 */
	boolean containsKey(final TKey key);

	/**
	 * Gets the entry set of the delta map.
	 *
	 * @return The entry set.
	 */
	Set<Map.Entry<TKey, TValue>> entrySet();

	/**
	 * Gets the stream of values.
	 *
	 * @return The stream.
	 */
	Stream<TValue> streamValues();
}
