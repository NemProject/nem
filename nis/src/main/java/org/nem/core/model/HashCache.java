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
	private static final int MinRetentionHours = 36;
	private final ConcurrentHashMap<Hash, HashMetaData> hashMap;
	private int retentionTime;

	/**
	 * Creates a hash cache.
	 */
	public HashCache() {
		this(50000, MinRetentionHours);
	}

	/**
	 * Creates a hash cache with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 * @param retentionTime The hash retention time (in hours).
	 */
	public HashCache(final int initialCapacity, final int retentionTime) {
		this.hashMap = new ConcurrentHashMap<>(initialCapacity);
		this.retentionTime = -1 == retentionTime ? -1 : Math.max(MinRetentionHours, retentionTime);
	}

	/**
	 * Gets the retention time.
	 *
	 * @return The retention time.
	 */
	public int getRetentionTime() {
		return this.retentionTime;
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
	 * Gets a value indicating whether or not the hash cache is empty.
	 *
	 * @return true if the hash cache is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return this.hashMap.isEmpty();
	}

	/**
	 * Clears the underlying hash map.
	 */
	public void clear() {
		this.hashMap.clear();
	}

	/**
	 * Gets the meta data corresponding to the given hash.
	 *
	 * @param hash The hash.
	 * @return The meta data.
	 */
	public HashMetaData get(final Hash hash) {
		return this.hashMap.get(hash);
	}

	/**
	 * Adds a new hash/meta data pair to the cache if hash is unknown.
	 *
	 * @param pair The pair.
	 */
	public void put(final HashMetaDataPair pair) {
		final HashMetaData original = this.hashMap.putIfAbsent(pair.getHash(), pair.getMetaData());
		if (null != original) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}
	}

	/**
	 * Adds new hash/meta data pairs to the cache if hash is unknown.
	 * Throws if any of the hashes is already in the cache.
	 *
	 * @param pairs The pairs to add.
	 */
	public void putAll(final List<HashMetaDataPair> pairs) {
		for (HashMetaDataPair pair : pairs) {
			final HashMetaData original = this.hashMap.putIfAbsent(pair.getHash(), pair.getMetaData());
			if (null != original) {
				throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
			}
		}
	}

	/**
	 * Removes a hash/meta data pair from the cache.
	 *
	 * @param hash The hash to remove.
	 */
	public void remove(final Hash hash) {
		this.hashMap.remove(hash);
	}

	/**
	 * Removes hash/meta data pairs from the cache.
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
		if (-1 != this.retentionTime) {
			this.hashMap.entrySet().removeIf(entry -> entry.getValue().getTimeStamp().compareTo(timeStamp) < 0);
		}
	}

	/**
	 * Creates a deep copy of this hash cache.
	 *
	 * @return The copy of this hash cache.
	 */
	public HashCache copy() {
		// note that this is really creating a shallow copy, which has the effect of a deep copy
		// because hash map keys and values are immutable
		final HashCache cache = new HashCache(this.size(), this.getRetentionTime());
		cache.hashMap.putAll(this.hashMap);
		return cache;
	}

	/**
	 * Copies this hash cash to another cache.
	 *
	 * @param cache The hash cache to copy into.
	 */
	public void shallowCopyTo(final HashCache cache) {
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
		cache.retentionTime = this.retentionTime;
	}

	/**
	 * Returns a stream of map entries.
	 *
	 * @return The stream.
	 */
	public Stream<Map.Entry<Hash, HashMetaData>> stream() {
		return this.hashMap.entrySet().stream();
	}
}
