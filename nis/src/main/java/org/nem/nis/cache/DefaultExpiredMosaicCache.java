package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.delta.*;
import org.nem.nis.state.ReadOnlyMosaicBalances;

import java.util.*;

/**
 * Default implementation of an expired mosaic cache.
 */
public class DefaultExpiredMosaicCache implements ExpiredMosaicCache, DeepCopyableCache<DefaultExpiredMosaicCache>, CommittableCache {
	private final MutableObjectAwareDeltaMap<BlockHeight, ExpiredMosaicBlockGroup> map;
	private boolean isCopy = false;

	public DefaultExpiredMosaicCache() {
		this(new MutableObjectAwareDeltaMap<>(100));
	}

	private DefaultExpiredMosaicCache(final MutableObjectAwareDeltaMap<BlockHeight, ExpiredMosaicBlockGroup> map) {
		this.map = map;
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public int deepSize() {
		return this.map.readOnlyEntrySet().stream().map(Map.Entry::getValue).map(group -> group.expiredMosaicsCount()).reduce(0, Integer::sum);
	}

	@Override
	public Collection<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> findExpirationsAtHeight(BlockHeight height) {
		final ExpiredMosaicBlockGroup group = this.map.getOrDefault(height, null);
		return null == group ? new ArrayList<>() : group.getAll();
	}

	@Override
	public void addExpiration(final BlockHeight height, final MosaicId mosaicId, final ReadOnlyMosaicBalances balances) {
		if (!this.map.containsKey(height)) {
			this.map.put(height, new ExpiredMosaicBlockGroup());
		}

		this.map.get(height).addExpiredMosaic(mosaicId, balances);
	}

	@Override
	public void removeAll(final BlockHeight height) {
		this.map.remove(height);
	}

	// region DeepCopyableCache

	@Override
	public void shallowCopyTo(final DefaultExpiredMosaicCache rhs) {
		this.map.shallowCopyTo(rhs.map);
	}

	@Override
	public DefaultExpiredMosaicCache copy() {
		if (this.isCopy) {
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all
		final DefaultExpiredMosaicCache copy = new DefaultExpiredMosaicCache(this.map.rebase());
		copy.isCopy = true;
		return copy;
	}

	@Override
	public DefaultExpiredMosaicCache deepCopy() {
		return new DefaultExpiredMosaicCache(this.map.deepCopy());
	}

	// endregion

	// region CommitableCache

	@Override
	public void commit() {
		this.map.commit();
	}

	// endregion

	// region ExpiredMosaicBlockGroup

	private static class ExpiredMosaicBlockGroup implements Copyable<ExpiredMosaicBlockGroup> {
		private final Map<MosaicId, ReadOnlyMosaicBalances> expiredMosaics = new HashMap<>();

		public int expiredMosaicsCount() {
			return this.expiredMosaics.size();
		}

		public Collection<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> getAll() {
			return this.expiredMosaics.entrySet();
		}

		public void addExpiredMosaic(final MosaicId mosaicId, final ReadOnlyMosaicBalances balances) {
			this.expiredMosaics.put(mosaicId, balances);
		}

		@Override
		public ExpiredMosaicBlockGroup copy() {
			// notice that both keys and values are immutable, so no additional copies are needed
			final ExpiredMosaicBlockGroup copy = new ExpiredMosaicBlockGroup();
			this.expiredMosaics.entrySet().forEach(e -> copy.expiredMosaics.put(e.getKey(), e.getValue()));
			return copy;
		}
	}

	// endregion
};
