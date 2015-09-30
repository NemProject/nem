package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class DefaultHashCache implements HashCache, CopyableCache<DefaultHashCache>, CommittableCache {
	private static final int MIN_RETENTION_HOURS = 36;
	private static final int INITIAL_CAPACITY = 50000;
	private final Map<Hash, HashMetaData> hashMap;
	private final Map<Hash, HashMetaData> addedHashes;
	private final Map<Hash, HashMetaData> removedHashes;
	private int retentionTime;
	private boolean isCopy = false;

	/**
	 * Creates a hash cache.
	 */
	public DefaultHashCache() {
		this(INITIAL_CAPACITY, MIN_RETENTION_HOURS);
	}

	/**
	 * Creates a hash cache with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 * @param retentionTime The hash retention time (in hours).
	 */
	public DefaultHashCache(final int initialCapacity, final int retentionTime) {
		this(
				retentionTime,
				new ConcurrentHashMap<>(initialCapacity),
				Collections.emptyMap(),
				Collections.emptyMap());
	}

	private DefaultHashCache(
			final int retentionTime,
			final Map<Hash, HashMetaData> hashMap,
			final Map<Hash, HashMetaData> addedHashes,
			final Map<Hash, HashMetaData> removedHashes) {
		this.hashMap = hashMap;
		this.addedHashes = addedHashes;
		this.removedHashes = removedHashes;
		this.retentionTime = -1 == retentionTime ? -1 : Math.max(MIN_RETENTION_HOURS, retentionTime);
	}

	@Override
	public int getRetentionTime() {
		return this.retentionTime;
	}

	@Override
	public int size() {
		return this.hashMap.size() + this.addedHashes.size() - this.removedHashes.size();
	}

	@Override
	public void clear() {
		this.removedHashes.putAll(this.hashMap);
		this.removedHashes.putAll(this.addedHashes);
		this.addedHashes.clear();
	}

	@Override
	public HashMetaData get(final Hash hash) {
		if (this.removedHashes.containsKey(hash)) {
			return null;
		}

		return this.hashMap.containsKey(hash) ? this.hashMap.get(hash) : this.addedHashes.get(hash);
	}

	@Override
	public void put(final HashMetaDataPair pair) {
		final Hash hash = pair.getHash();
		if (this.removedHashes.containsKey(hash)) {
			this.removedHashes.remove(hash);
			if (!this.hashMap.containsKey(hash)) {
				this.addedHashes.put(hash, pair.getMetaData());
			}

			return;
		}

		if (this.hashMap.containsKey(hash) || this.addedHashes.containsKey(hash)) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}

		this.addedHashes.put(hash, pair.getMetaData());
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		pairs.forEach(this::put);
	}

	@Override
	public void remove(final Hash hash) {
		if (this.removedHashes.containsKey(hash)) {
			return;
		}

		if (this.hashMap.containsKey(hash)) {
			this.removedHashes.put(hash, this.hashMap.get(hash));
			return;
		}

		if (this.addedHashes.containsKey(hash)) {
			this.addedHashes.remove(hash);
		}
	}

	@Override
	public void removeAll(final List<Hash> hashes) {
		hashes.forEach(this::remove);
	}

	@Override
	public boolean hashExists(final Hash hash) {
		return !this.removedHashes.containsKey(hash) &&
				(this.hashMap.containsKey(hash) || this.addedHashes.containsKey(hash));
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes) {
		for (final Hash hash : hashes) {
			if (this.hashExists(hash)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void prune(final TimeInstant timeStamp) {
		if (-1 == this.retentionTime) {
			return;
		}

		final TimeInstant pruneTime = this.getPruneTime(timeStamp);
		final Collection<HashMetaDataPair> pairs = this.getAllBefore(this.hashMap, pruneTime);
		pairs.forEach(pair -> this.removedHashes.put(pair.getHash(), pair.getMetaData()));
		this.addedHashes.entrySet().removeIf(entry -> entry.getValue().getTimeStamp().compareTo(pruneTime) < 0);
	}

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

	// region CopyableCache

	@Override
	public DefaultHashCache copy() {
		if (this.isCopy) {
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all.
		final DefaultHashCache copy = new DefaultHashCache(
				this.retentionTime,
				this.hashMap,
				new ConcurrentHashMap<>(INITIAL_CAPACITY),
				new ConcurrentHashMap<>(INITIAL_CAPACITY));
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void shallowCopyTo(final DefaultHashCache cache) {
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
		cache.addedHashes.clear();
		cache.addedHashes.putAll(this.addedHashes);
		cache.removedHashes.clear();
		cache.removedHashes.putAll(this.removedHashes);
		cache.retentionTime = this.retentionTime;
	}

	// rendregion

	// region CommitableCache

	@Override
	public void commit() {
		this.hashMap.putAll(this.addedHashes);
		this.removedHashes.keySet().forEach(this.hashMap::remove);
		this.addedHashes.clear();
		this.removedHashes.clear();
	}

	// endregion

	/**
	 * Creates a deep copy of this hash cache.
	 *
	 * @return The deep copy.
	 */
	public DefaultHashCache deepCopy() {
		if (this.isCopy) {
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		return new DefaultHashCache(
				this.retentionTime,
				new ConcurrentHashMap<>(this.hashMap),
				Collections.emptyMap(),
				Collections.emptyMap());
	}
}
