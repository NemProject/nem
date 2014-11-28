package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.time.TimeInstant;

import java.util.concurrent.ConcurrentHashMap;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class HashCache {
	private final ConcurrentHashMap<Hash, TimeInstant> hashMap = new ConcurrentHashMap<>(50000);

	/**
	 * Gets the size of the underlying hash map.
	 * @return The size.
	 */
	public int size() {
		return this.hashMap.size();
	}

	/**
	 * Adds a new hash/time stamp pair to the cache if hash is unknown.
	 *
	 * @param hash The hash to add.
	 * @param timeStamp The time stamp.
	 */
	public void add(final Hash hash, final TimeInstant timeStamp) {
		final TimeInstant original = this.hashMap.putIfAbsent(hash, timeStamp);
		if (null != original) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", hash));
		}
	}

	/**
	 * Gets a value indicating whether or not a given hash is in the cache.
	 *
	 * @param hash The hash to check.
	 * @return true if the hash is already in the cache, false otherwise.
	 */
	public boolean isKnown(final Hash hash) {
		return this.hashMap.containsKey(hash);
	}

	/**
	 * Removes all elements that have time stamp prior to the given time stamp.
	 *
	 * @param timeStamp The time stamp.
	 */
	public void prune(final TimeInstant timeStamp) {
		this.hashMap.entrySet().removeIf(entry -> entry.getValue().compareTo(timeStamp) < 0);
	}
}
