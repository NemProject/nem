package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.delta.DeltaMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class DefaultHashCache implements HashCache, CopyableCache<DefaultHashCache>, CommittableCache {
	private static final int MIN_RETENTION_HOURS = 36;
	private static final int INITIAL_CAPACITY = 50000;
	private final DeltaMap<Hash, HashMetaData> map;
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
		this(new DeltaMap<>(initialCapacity), retentionTime);
	}

	private DefaultHashCache(
			final DeltaMap<Hash, HashMetaData> map,
			final int retentionTime) {
		this.map = map;
		this.retentionTime = -1 == retentionTime ? -1 : Math.max(MIN_RETENTION_HOURS, retentionTime);
	}

	@Override
	public int getRetentionTime() {
		return this.retentionTime;
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public void clear() {
		this.map.clear();
	}

	@Override
	public HashMetaData get(final Hash hash) {
		return this.map.get(hash);
	}

	@Override
	public void put(final HashMetaDataPair pair) {
		if (null != this.get(pair.getHash())) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}

		this.map.put(pair.getHash(), pair.getMetaData());
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		pairs.forEach(this::put);
	}

	@Override
	public void remove(final Hash hash) {
		this.map.remove(hash);
	}

	@Override
	public void removeAll(final List<Hash> hashes) {
		hashes.forEach(this::remove);
	}

	@Override
	public boolean hashExists(final Hash hash) {
		return this.map.containsKey(hash);
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes) {
		return hashes.stream().anyMatch(this::hashExists);
	}

	@Override
	public void prune(final TimeInstant timeStamp) {
		if (-1 == this.retentionTime) {
			return;
		}

		final TimeInstant pruneTime = this.getPruneTime(timeStamp);
		final Collection<HashMetaDataPair> pairs = this.getAllBefore(pruneTime);
		pairs.forEach(pair -> this.remove(pair.getHash()));
	}

	private Collection<HashMetaDataPair> getAllBefore(final TimeInstant time) {
		return this.map.entrySet().stream()
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
		final DefaultHashCache copy = new DefaultHashCache(this.map.rebase(), this.retentionTime);
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void shallowCopyTo(final DefaultHashCache cache) {
		this.map.shallowCopyTo(cache.map);
		cache.retentionTime = this.retentionTime;
	}

	// rendregion

	// region CommitableCache

	@Override
	public void commit() {
		this.map.commit();
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

		return new DefaultHashCache(this.map.deepCopy(), this.retentionTime);
	}
}
