package org.nem.nis.cache;

import org.nem.core.model.mosaic.Mosaic;

/**
 * A synchronized mosaic cache implementation.
 */
public class SynchronizedMosaicCache implements MosaicCache, CopyableCache<SynchronizedMosaicCache> {
	private final DefaultMosaicCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache The wrapped cache.
	 */
	public SynchronizedMosaicCache(final DefaultMosaicCache cache) {
		this.cache = cache;
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.cache.size();
		}
	}

	@Override
	public Mosaic get(final String id) {
		synchronized (this.lock) {
			return this.cache.get(id);
		}
	}

	@Override
	public boolean contains(final String id) {
		synchronized (this.lock) {
			return this.cache.contains(id);
		}
	}

	@Override
	public void add(final Mosaic mosaic) {
		synchronized (this.lock) {
			this.cache.add(mosaic);
		}
	}

	@Override
	public void remove(final Mosaic mosaic) {
		synchronized (this.lock) {
			this.cache.remove(mosaic);
		}
	}

	@Override
	public void shallowCopyTo(final SynchronizedMosaicCache rhs) {
		synchronized (rhs.lock) {
			this.cache.shallowCopyTo(rhs.cache);
		}
	}

	@Override
	public SynchronizedMosaicCache copy() {
		synchronized (this.lock) {
			return new SynchronizedMosaicCache(this.cache.copy());
		}
	}
}
