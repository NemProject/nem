package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

/**
 * A synchronized smart tile supply cache implementation.
 */
public class SynchronizedSmartTileSupplyCache implements SmartTileSupplyCache, CopyableCache<SynchronizedSmartTileSupplyCache> {
	private final DefaultSmartTileSupplyCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache The wrapped cache.
	 */
	public SynchronizedSmartTileSupplyCache(final DefaultSmartTileSupplyCache cache) {
		this.cache = cache;
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.cache.size();
		}
	}

	@Override
	public Quantity getCurrentSupply(final MosaicId mosaicId) {
		synchronized (this.lock) {
			return this.cache.getCurrentSupply(mosaicId);
		}
	}

	@Override
	public Quantity increaseSupply(final SmartTile smartTile) {
		synchronized (this.lock) {
			return this.cache.increaseSupply(smartTile);
		}
	}

	@Override
	public Quantity decreaseSupply(final SmartTile smartTile) {
		synchronized (this.lock) {
			return this.cache.decreaseSupply(smartTile);
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedSmartTileSupplyCache rhs) {
		synchronized (rhs.lock) {
			this.cache.shallowCopyTo(rhs.cache);
		}
	}

	@Override
	public SynchronizedSmartTileSupplyCache copy() {
		synchronized (this.lock) {
			return new SynchronizedSmartTileSupplyCache(this.cache.copy());
		}
	}
}
