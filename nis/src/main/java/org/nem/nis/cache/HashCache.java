package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.HashMetaDataPair;
import org.nem.core.time.TimeInstant;

import java.util.List;

/**
 * A transaction hash cache.
 * TODO 20141212 J-J: consider pruning the hashcache interfaces.
 */
public interface HashCache extends ReadOnlyHashCache {

	/**
	 * Removes all elements that have time stamp prior to the given time stamp.
	 *
	 * @param timeStamp The time stamp.
	 */
	public void prune(final TimeInstant timeStamp);

	/**
	 * Adds a new hash/meta data pair to the cache if hash is unknown.
	 *
	 * @param pair The pair.
	 */
	public void put(final HashMetaDataPair pair);

	/**
	 * Adds new hash/meta data pairs to the cache if hash is unknown.
	 * Throws if any of the hashes is already in the cache.
	 *
	 * @param pairs The pairs to add.
	 */
	public void putAll(final List<HashMetaDataPair> pairs);

	/**
	 * Removes a hash/meta data pair from the cache.
	 *
	 * @param hash The hash to remove.
	 */
	public void remove(final Hash hash);

	/**
	 * Removes hash/meta data pairs from the cache.
	 *
	 * @param hashes The list of hashes to remove.
	 */
	public void removeAll(final List<Hash> hashes);

	/**
	 * Clears the underlying hash map.
	 */
	public void clear();
}
