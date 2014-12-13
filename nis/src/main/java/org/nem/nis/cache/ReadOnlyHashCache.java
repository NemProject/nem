package org.nem.nis.cache;

import org.nem.core.crypto.Hash;

import java.util.Collection;

/**
 * A readonly transaction hash cache.
 */
public interface ReadOnlyHashCache {

	/**
	 * Gets a value indicating whether or not any of a given collection of hashes is in the cache.
	 *
	 * @param hashes The collection of hashes to check.
	 * @return true if any of the given hashes is already in the cache, false otherwise.
	 */
	public boolean anyHashExists(final Collection<Hash> hashes);

	// TODO 20141212 J-J: can this be removed?
	/**
	 * Gets the retention time.
	 *
	 * @return The retention time.
	 */
	public int getRetentionTime();
}
