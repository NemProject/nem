package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class DefaultHashCache implements HashCache, CopyableCache<DefaultHashCache> {
	private static final int MIN_RETENTION_HOURS = 36;
	private final ConcurrentHashMap<Hash, HashMetaData> hashMap;
	private int retentionTime;

	/**
	 * Creates a hash cache.
	 */
	public DefaultHashCache() {
		this(50000, MIN_RETENTION_HOURS);
	}

	/**
	 * Creates a hash cache with the specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 * @param retentionTime The hash retention time (in hours).
	 */
	public DefaultHashCache(final int initialCapacity, final int retentionTime) {
		this.hashMap = new ConcurrentHashMap<>(initialCapacity);
		this.retentionTime = -1 == retentionTime ? -1 : Math.max(MIN_RETENTION_HOURS, retentionTime);
	}

	@Override
	public int getRetentionTime() {
		return this.retentionTime;
	}

	@Override
	public int size() {
		return this.hashMap.size();
	}

	@Override
	public void clear() {
		this.hashMap.clear();
	}

	@Override
	public HashMetaData get(final Hash hash) {
		return this.hashMap.get(hash);
	}

	@Override
	public void put(final HashMetaDataPair pair) {
		final HashMetaData original = this.hashMap.putIfAbsent(pair.getHash(), pair.getMetaData());
		if (null != original) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		for (final HashMetaDataPair pair : pairs) {
			final HashMetaData original = this.hashMap.putIfAbsent(pair.getHash(), pair.getMetaData());
			if (null != original) {
				throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
			}
		}
	}

	@Override
	public void remove(final Hash hash) {
		this.hashMap.remove(hash);
	}

	@Override
	public void removeAll(final List<Hash> hashes) {
		hashes.stream().forEach(this::remove);
	}

	@Override
	public boolean hashExists(final Hash hash) {
		return this.hashMap.containsKey(hash);
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes) {
		for (final Hash hash : hashes) {
			if (this.hashMap.containsKey(hash)) {
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
		this.hashMap.entrySet().removeIf(entry -> entry.getValue().getTimeStamp().compareTo(pruneTime) < 0);
	}

	private TimeInstant getPruneTime(final TimeInstant currentTime) {
		final TimeInstant retentionTime = TimeInstant.ZERO.addHours(this.retentionTime);
		return new TimeInstant(currentTime.compareTo(retentionTime) <= 0 ? 0 : currentTime.subtract(retentionTime));
	}

	@Override
	public DefaultHashCache copy() {
		// note that this is really creating a shallow copy, which has the effect of a deep copy
		// because hash map keys and values are immutable
		final DefaultHashCache cache = new DefaultHashCache(this.size(), this.getRetentionTime());
		cache.hashMap.putAll(this.hashMap);
		return cache;
	}

	@Override
	public void shallowCopyTo(final DefaultHashCache cache) {
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
		cache.retentionTime = this.retentionTime;
	}

	public DefaultHashCacheCopy smartCopy() {
		// note that this is not copying at all.
		return new DefaultHashCacheCopy(this.hashMap, this.getRetentionTime());
	}

}
