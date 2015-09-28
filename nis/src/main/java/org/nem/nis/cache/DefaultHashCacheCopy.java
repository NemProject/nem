package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a copy of a hash cache.
 */
public class DefaultHashCacheCopy implements HashCache, CommittableCache {
	private final Map<Hash, HashMetaData> original;
	private final Map<Hash, HashMetaData> addedHashes;
	private final Map<Hash, HashMetaData> removedHashes;
	private final int retentionTime;

	/**
	 * Creates a new default hash cache copy.
	 *
	 * @param original The hash map containing the original keys and values
	 * @param retentionTime The retention time.
	 */
	public DefaultHashCacheCopy(final Map<Hash, HashMetaData> original, final int retentionTime) {
		this.original = original;
		this.addedHashes = new ConcurrentHashMap<>(50000);
		this.removedHashes = new ConcurrentHashMap<>(50000);
		this.retentionTime = retentionTime;
	}

	/**
	 * Creates a new default hash cache copy.
	 * This constructor is used for tests.
	 *
	 * @param original The hash map containing the original keys and values
	 * @param retentionTime The retention time.
	 */
	public DefaultHashCacheCopy(
			final Map<Hash, HashMetaData> original,
			final Map<Hash, HashMetaData> addedHashes,
			final Map<Hash, HashMetaData> removedHashes,
			final int retentionTime) {
		this.original = original;
		this.addedHashes = addedHashes;
		this.removedHashes = removedHashes;
		this.retentionTime = retentionTime;
	}

	// region HashCache

	@Override
	public void prune(TimeInstant timeStamp) {
		if (-1 == this.retentionTime) {
			return;
		}

		final TimeInstant pruneTime = this.getPruneTime(timeStamp);
		final Collection<HashMetaDataPair> pairs = this.getAllBefore(this.original, pruneTime);
		pairs.forEach(pair -> this.removedHashes.put(pair.getHash(), pair.getMetaData()));
		this.addedHashes.entrySet().removeIf(entry -> entry.getValue().getTimeStamp().compareTo(pruneTime) < 0);
	}

	@Override
	public void put(HashMetaDataPair pair) {
		final Hash hash = pair.getHash();
		if (this.removedHashes.containsKey(hash)) {
			this.removedHashes.remove(hash);
			if (!this.original.containsKey(hash)) {
				this.addedHashes.put(hash, pair.getMetaData());
			}

			return;
		}

		if (this.original.containsKey(hash) || this.addedHashes.containsKey(hash)) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}

		this.addedHashes.put(hash, pair.getMetaData());
	}

	@Override
	public void putAll(List<HashMetaDataPair> pairs) {
		pairs.forEach(this::put);
	}

	@Override
	public void remove(Hash hash) {
		if (this.removedHashes.containsKey(hash)) {
			return;
		}

		if (this.original.containsKey(hash)) {
			this.removedHashes.put(hash, this.original.get(hash));
			return;
		}

		if (this.addedHashes.containsKey(hash)) {
			this.addedHashes.remove(hash);
		}
	}

	@Override
	public void removeAll(List<Hash> hashes) {
		hashes.forEach(this::remove);
	}

	@Override
	public void clear() {
		this.removedHashes.putAll(this.original);
		this.removedHashes.putAll(this.addedHashes);
		this.addedHashes.clear();
	}

	// endregion

	// region ReadOnlyHashCache

	@Override
	public int getRetentionTime() {
		return this.retentionTime;
	}

	@Override
	public int size() {
		return this.original.size() + this.addedHashes.size() - this.removedHashes.size();
	}

	@Override
	public HashMetaData get(Hash hash) {
		if (this.removedHashes.containsKey(hash)) {
			return null;
		}

		return this.original.containsKey(hash) ? this.original.get(hash) : this.addedHashes.get(hash);
	}

	@Override
	public boolean hashExists(Hash hash) {
		return !this.removedHashes.containsKey(hash) &&
				(this.original.containsKey(hash) || this.addedHashes.containsKey(hash));

	}

	@Override
	public boolean anyHashExists(Collection<Hash> hashes) {
		for (final Hash hash : hashes) {
			if (this.hashExists(hash)) {
				return true;
			}
		}

		return false;
	}

	// endregion

	// region CommitableCache

	@Override
	public void commit() {
		this.original.putAll(this.addedHashes);
		this.removedHashes.keySet().forEach(this.original::remove);
	}

	// endregion

	private Collection<HashMetaDataPair> getAllBefore(final Map<Hash, HashMetaData> map, final TimeInstant time) {
		return map.entrySet().stream()
				.filter(entry -> entry.getValue().getTimeStamp().compareTo(time) < 0)
				.map(entry -> new HashMetaDataPair(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private TimeInstant getPruneTime(final TimeInstant currentTime) {
		final TimeInstant retentionTime = TimeInstant.ZERO.addHours(this.retentionTime);
		return new TimeInstant(currentTime.compareTo(retentionTime) <= 0 ? 0 : currentTime.subtract(retentionTime));
	}
}
