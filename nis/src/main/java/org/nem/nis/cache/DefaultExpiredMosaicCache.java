package org.nem.nis.cache;

import java.util.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.delta.*;
import org.nem.nis.state.*;

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
		return this.map.readOnlyEntrySet().stream().map(Map.Entry::getValue).map(group -> group.expiredMosaicsCount()).reduce(0,
				Integer::sum);
	}

	@Override
	public Collection<ExpiredMosaicEntry> findExpirationsAtHeight(BlockHeight height) {
		final ExpiredMosaicBlockGroup group = this.map.getOrDefault(height, null);
		return null == group ? new ArrayList<>() : group.getAll();
	}

	@Override
	public void addExpiration(final BlockHeight height, final MosaicId mosaicId, final ReadOnlyMosaicBalances balances,
			final ExpiredMosaicType expirationType) {
		if (!this.map.containsKey(height)) {
			this.map.put(height, new ExpiredMosaicBlockGroup());
		}

		// make a deep copy of balances to disconnect historical record (stored in this cache) from current state
		final MosaicBalances balancesCopy = new MosaicBalances();
		balances.getOwners().forEach(owner -> balancesCopy.incrementBalance(owner, balances.getBalance(owner)));

		this.map.get(height).addExpiredMosaic(new ExpiredMosaicEntry(mosaicId, balancesCopy, expirationType));
	}

	@Override
	public void removeExpiration(final BlockHeight height, final MosaicId mosaicId) {
		final ExpiredMosaicBlockGroup group = this.map.getOrDefault(height, null);
		if (null != group) {
			group.removeExpiredMosaic(mosaicId);

			if (0 == group.expiredMosaicsCount()) {
				this.map.remove(height);
			}
		}
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
		private final List<ExpiredMosaicEntry> expiredMosaicEntries = new ArrayList<>();

		public int expiredMosaicsCount() {
			return this.expiredMosaicEntries.size();
		}

		public Collection<ExpiredMosaicEntry> getAll() {
			return this.expiredMosaicEntries;
		}

		public void addExpiredMosaic(final ExpiredMosaicEntry entry) {
			this.expiredMosaicEntries.add(entry);
		}

		public void removeExpiredMosaic(final MosaicId mosaicId) {
			this.expiredMosaicEntries.removeIf(entry -> entry.getMosaicId().equals(mosaicId));
		}

		@Override
		public ExpiredMosaicBlockGroup copy() {
			// notice that values are immutable, so no additional copies are needed
			final ExpiredMosaicBlockGroup copy = new ExpiredMosaicBlockGroup();
			this.expiredMosaicEntries.forEach(entry -> copy.expiredMosaicEntries.add(entry));
			return copy;
		}
	}

	// endregion
};
