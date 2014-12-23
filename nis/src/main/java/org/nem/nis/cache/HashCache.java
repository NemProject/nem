package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.HashMetaDataPair;
import org.nem.core.time.TimeInstant;

import java.util.List;

/**
 * A transaction hash cache.
 * TODO 20141212 J-B: i'm not sure if it makes sense for this object to know about it's retention time
 * > since it isn't using it. I think it might make more sense for either (1) the caller to manage the retention time
 * > or (2) this object to do the retention time calculation in prune. thoughts?
 * TODO 20141222 BR -> J: I don't like (1) because 2 callers could call with different retention times on the same cache.
 * > If caller 1 relies on 12 hours cache and caller 2 on only 3 hours, caller 1 could experience a disaster.
 * > So I would prefer (2), pass the current time, the cache calculates the pruning time and does the pruning.
 * TODO 20141222 BR -> J: i like (2) as well
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
