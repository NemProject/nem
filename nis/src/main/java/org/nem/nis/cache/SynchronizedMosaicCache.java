package org.nem.nis.cache;

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
