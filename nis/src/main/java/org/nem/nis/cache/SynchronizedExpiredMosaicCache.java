package org.nem.nis.cache;

import java.util.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

/**
 * A synchronized ExpiredMosaicCache implementation.
 */
public class SynchronizedExpiredMosaicCache
		implements
			ExpiredMosaicCache,
			DeepCopyableCache<SynchronizedExpiredMosaicCache>,
			CommittableCache {
	private final DefaultExpiredMosaicCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache Expired moasaic cache.
	 */
	public SynchronizedExpiredMosaicCache(final DefaultExpiredMosaicCache cache) {
		this.cache = cache;
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.cache.size();
		}
	}

	@Override
	public int deepSize() {
		synchronized (this.lock) {
			return this.cache.deepSize();
		}
	}

	@Override
	public Collection<ExpiredMosaicEntry> findExpirationsAtHeight(BlockHeight height) {
		synchronized (this.lock) {
			return this.cache.findExpirationsAtHeight(height);
		}
	}

	@Override
	public void addExpiration(final BlockHeight height, final MosaicId mosaicId, final ReadOnlyMosaicBalances balances,
			final ExpiredMosaicType expirationType) {
		synchronized (this.lock) {
			this.cache.addExpiration(height, mosaicId, balances, expirationType);
		}
	}

	@Override
	public void removeExpiration(final BlockHeight height, final MosaicId mosaicId) {
		synchronized (this.lock) {
			this.cache.removeExpiration(height, mosaicId);
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedExpiredMosaicCache rhs) {
		synchronized (rhs.lock) {
			this.cache.shallowCopyTo(rhs.cache);
		}
	}

	@Override
	public SynchronizedExpiredMosaicCache copy() {
		synchronized (this.lock) {
			return new SynchronizedExpiredMosaicCache(this.cache.copy());
		}
	}

	@Override
	public SynchronizedExpiredMosaicCache deepCopy() {
		synchronized (this.lock) {
			return new SynchronizedExpiredMosaicCache(this.cache.deepCopy());
		}
	}

	@Override
	public void commit() {
		synchronized (this.lock) {
			this.cache.commit();
		}
	}
}
