package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.delta.*;

import java.util.*;

/**
 * General class for holding hashes and checking for duplicate hashes. Supports pruning.
 */
public class DefaultHashCache implements HashCache, CopyableCache<DefaultHashCache>, CommittableCache {
	private static final int MIN_RETENTION_HOURS = 36;
	private static final int INITIAL_CAPACITY = 50000;
	private final ImmutableObjectDeltaMap<Hash, HashMetaData> map;
	private final SkipListDeltaMap<TimeInstant, Hash> navigationalMap;
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
		this(new ImmutableObjectDeltaMap<>(initialCapacity), new SkipListDeltaMap<>(), retentionTime);
	}

	private DefaultHashCache(final ImmutableObjectDeltaMap<Hash, HashMetaData> map,
			final SkipListDeltaMap<TimeInstant, Hash> navigationalMap, final int retentionTime) {
		this.map = map;
		this.navigationalMap = navigationalMap;
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
		this.navigationalMap.clear();
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

		final HashMetaData metaData = pair.getMetaData();
		final Hash hash = pair.getHash();
		this.map.put(hash, metaData);
		this.navigationalMap.put(metaData.getTimeStamp(), hash);
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		pairs.forEach(this::put);
	}

	@Override
	public void remove(final Hash hash) {
		final HashMetaData metaData = this.map.get(hash);
		if (null == metaData) {
			return;
		}

		this.map.remove(hash);
		this.navigationalMap.remove(metaData.getTimeStamp(), hash);
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
		final Collection<Hash> map = this.navigationalMap.getValuesBefore(pruneTime);
		map.forEach(this::remove);
	}

	private TimeInstant getPruneTime(final TimeInstant currentTime) {
		final TimeInstant retentionTime = TimeInstant.ZERO.addHours(this.retentionTime);
		return new TimeInstant(currentTime.compareTo(retentionTime) <= 0 ? 0 : currentTime.subtract(retentionTime));
	}

	// region CopyableCache

	@Override
	public DefaultHashCache copy() {
		if (this.isCopy) {
			// TODO 20151013 J-J: add test for this case
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all.
		final DefaultHashCache copy = new DefaultHashCache(this.map.rebase(), this.navigationalMap.rebase(), this.retentionTime);
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void shallowCopyTo(final DefaultHashCache cache) {
		this.map.shallowCopyTo(cache.map);
		this.navigationalMap.shallowCopyTo(cache.navigationalMap);
		cache.retentionTime = this.retentionTime;
	}

	// endregion

	// region CommitableCache

	@Override
	public void commit() {
		this.map.commit();
		this.navigationalMap.commit();
	}

	// endregion

	/**
	 * Creates a deep copy of this hash cache.
	 *
	 * @return The deep copy.
	 */
	public DefaultHashCache deepCopy() {
		// TODO 20151013 J-J: add test for deepCopy
		if (this.isCopy) {
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		return new DefaultHashCache(this.map.deepCopy(), this.navigationalMap.deepCopy(), this.retentionTime);
	}
}
