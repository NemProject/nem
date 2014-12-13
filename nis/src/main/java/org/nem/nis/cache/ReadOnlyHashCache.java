package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.HashMetaData;

import java.util.Collection;

/**
 * A readonly transaction hash cache.
 */
public interface ReadOnlyHashCache {

	/**
	 * Gets the retention time.
	 *
	 * @return The retention time.
	 */
	public int getRetentionTime();

	/**
	 * Gets a value indicating whether or not the hash cache is empty.
	 *
	 * @return true if the hash cache is empty, false otherwise.
	 */
	public boolean isEmpty();

	/**
	 * Gets the size of the underlying hash map.
	 *
	 * @return The size.
	 */
	public int size();

	/**
	 * Gets the meta data corresponding to the given hash.
	 *
	 * @param hash The hash.
	 * @return The meta data.
	 */
	public HashMetaData get(final Hash hash);

	/**
	 * Gets a value indicating whether or not a given hash is in the cache.
	 *
	 * @param hash The hash to check.
	 * @return true if the hash is already in the cache, false otherwise.
	 */
	public boolean hashExists(final Hash hash);

	/**
	 * Gets a value indicating whether or not any of a given collection of hashes is in the cache.
	 *
	 * @param hashes The collection of hashes to check.
	 * @return true if any of the given hashes is already in the cache, false otherwise.
	 */
	public boolean anyHashExists(final Collection<Hash> hashes);
}
