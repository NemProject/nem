package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class HashCache {
	private final ConcurrentHashMap<Hash, TimeInstant> hashMap;

	public HashCache() {
		this(50000);
	}

	public HashCache(final int initialCapacity) {
		this.hashMap = new ConcurrentHashMap<>(initialCapacity);
	}

	/**
	 * Gets the size of the underlying hash map.
	 *
	 * @return The size.
	 */
	public int size() {
		return this.hashMap.size();
	}

	/**
	 * Clears the underlying hash map.
	 */
	public void clear() {
		this.hashMap.clear();
	}

	/**
	 * Gets the time stamp corresponding to the given hash.
	 *
	 * @param hash The hash.
	 * @return The time stamp.
	 */
	public TimeInstant get(final Hash hash) {
		return this.hashMap.get(hash);
	}

	/**
	 * Adds a new hash/timestamp pair to the cache if hash is unknown.
	 *
	 * @param pair The pair..
	 */
	public void put(final HashTimeInstantPair pair) {
		final TimeInstant original = this.hashMap.putIfAbsent(pair.getHash(), pair.getTimeStamp());
		if (null != original) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}
	}

	/**
	 * Adds a new hash/time stamp pairs to the cache if hash is unknown.
	 * Throws if any of the hashes is already in the cache.
	 *
	 * @param pairs The pairs to add.
	 */
	public void putAll(final List<HashTimeInstantPair> pairs) {
		for (HashTimeInstantPair pair : pairs) {
			final TimeInstant original = this.hashMap.putIfAbsent(pair.getHash(), pair.getTimeStamp());
			if (null != original) {
				throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
			}
		}
	}

	/**
	 * Removes a hash/time stamp pair from the cache.
	 *
	 * @param hash The hash to remove.
	 */
	public void remove(final Hash hash) {
		this.hashMap.remove(hash);
	}

	/**
	 * Removes a hash/time stamp pair from the cache.
	 *
	 * @param hashes The list of hashes to remove.
	 */
	public void removeAll(final List<Hash> hashes) {
		hashes.stream().forEach(this::remove);
	}

	/**
	 * Gets a value indicating whether or not a given hash is in the cache.
	 *
	 * @param hash The hash to check.
	 * @return true if the hash is already in the cache, false otherwise.
	 */
	public boolean hashExists(final Hash hash) {
		return this.hashMap.containsKey(hash);
	}

	/**
	 * Gets a value indicating whether or not any of a given collection of hashes is in the cache.
	 *
	 * @param hashes The collection of hashes to check.
	 * @return true if any of the given hashes is already in the cache, false otherwise.
	 */
	public boolean anyHashExists(final Collection<Hash> hashes) {
		for (Hash hash : hashes) {
			if (this.hashMap.containsKey(hash)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes all elements that have time stamp prior to the given time stamp.
	 *
	 * @param timeStamp The time stamp.
	 */
	public void prune(final TimeInstant timeStamp) {
		this.hashMap.entrySet().removeIf(entry -> entry.getValue().compareTo(timeStamp) < 0);
	}

	/**
	 * Returns a shallow copy of this hash cache.
	 *
	 * @return the shallow copy of this hash cache.
	 */
	public HashCache shallowCopy() {
		final HashCache cache = new HashCache();
		cache.hashMap.putAll(this.hashMap);
		return cache;
	}

	/**
	 * Copies this hash cash to another cache.
	 *
	 * @param cache The hash cache to copy to.
	 */
	public void shallowCopyTo(final HashCache cache) {
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
	}

	/**
	 * Returns a stream of map entries.
	 *
	 * @return The stream.
	 */
	public Stream<Map.Entry<Hash, TimeInstant>> stream() {
		return this.hashMap.entrySet().stream();
	}
}
