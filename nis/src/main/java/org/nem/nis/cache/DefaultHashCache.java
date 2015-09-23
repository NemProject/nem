package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class DefaultHashCache implements HashCache, CopyableCache<DefaultHashCache> {
	private static final int MIN_RETENTION_HOURS = 36;
	private final ConcurrentHashMap<Hash, HashMetaData> hashMap;
	private ConcurrentHashMap<Hash, HashMetaData> immutableHashMap;
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
		this.immutableHashMap = new ConcurrentHashMap<>(initialCapacity);
		this.hashMap = new ConcurrentHashMap<>(initialCapacity);
		this.retentionTime = -1 == retentionTime ? -1 : Math.max(MIN_RETENTION_HOURS, retentionTime);
	}

	@Override
	public int immutableCacheSize() {
		return this.immutableHashMap.size();
	}

	@Override
	public int getRetentionTime() {
		return this.retentionTime;
	}

	@Override
	public int size() {
		return this.hashMap.size() + this.immutableHashMap.size();
	}

	@Override
	public void clear() {
		this.immutableHashMap.clear();
		this.hashMap.clear();
	}

	@Override
	public HashMetaData get(final Hash hash) {
		final HashMetaData metaData = this.hashMap.get(hash);
		return null != metaData ? metaData : this.immutableHashMap.get(hash);
	}

	@Override
	public void put(final HashMetaDataPair pair) {
		if (this.hashExists(pair.getHash())) {
			throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
		}

		this.hashMap.put(pair.getHash(), pair.getMetaData());
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		for (final HashMetaDataPair pair : pairs) {
			if (this.hashExists(pair.getHash())) {
				throw new IllegalArgumentException(String.format("hash %s already exists in cache", pair.getHash()));
			}

			this.hashMap.put(pair.getHash(), pair.getMetaData());
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
		return this.hashMap.containsKey(hash) || this.immutableHashMap.containsKey(hash);
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes) {
		for (final Hash hash : hashes) {
			if (this.hashMap.containsKey(hash) || this.immutableHashMap.containsKey(hash)) {
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

		final TimeInstant transferTime = this.getTransferTime(timeStamp);
		final Collection<HashMetaDataPair> pairs = this.hashMap.entrySet().stream()
				.filter(entry ->  entry.getValue().getTimeStamp().compareTo(transferTime) < 0)
				.map(entry -> new HashMetaDataPair(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
		pairs.stream().forEach(p -> {
			this.immutableHashMap.put(p.getHash(), p.getMetaData());
			this.hashMap.remove(p.getHash());
		});
		final TimeInstant pruneTime = this.getPruneTime(timeStamp);
		this.immutableHashMap.entrySet().removeIf(entry -> entry.getValue().getTimeStamp().compareTo(pruneTime) < 0);
	}

	private TimeInstant getTransferTime(final TimeInstant currentTime) {
		final BlockChainConfiguration configuration = NemGlobals.getBlockChainConfiguration();
		final int seconds = configuration.getBlockGenerationTargetTime() * configuration.getSyncBlockLimit();
		final TimeInstant limit = TimeInstant.ZERO.addSeconds(seconds);
		return new TimeInstant(currentTime.compareTo(limit) <= 0 ? 0 : currentTime.subtract(limit));
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
		cache.immutableHashMap = this.immutableHashMap;
		cache.hashMap.putAll(this.hashMap);
		return cache;
	}

	@Override
	public void shallowCopyTo(final DefaultHashCache cache) {
		cache.immutableHashMap = this.immutableHashMap;
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
		cache.retentionTime = this.retentionTime;
	}
}
