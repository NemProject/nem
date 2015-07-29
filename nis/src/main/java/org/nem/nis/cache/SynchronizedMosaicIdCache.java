package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.dbmodel.DbMosaicId;

/**
 * A synchronized mosaic id cache implementation.
 */
public class SynchronizedMosaicIdCache implements MosaicIdCache {
	private final DefaultMosaicIdCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache The wrapped cache.
	 */
	public SynchronizedMosaicIdCache(final DefaultMosaicIdCache cache) {
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
	public DbMosaicId get(final MosaicId mosaicId) {
		synchronized (this.lock) {
			return this.cache.get(mosaicId);
		}
	}

	@Override
	public MosaicId get(final DbMosaicId dbMosaicId) {
		synchronized (this.lock) {
			return this.cache.get(dbMosaicId);
		}
	}

	@Override
	public boolean contains(final MosaicId mosaicId) {
		synchronized (this.lock) {
			return this.cache.contains(mosaicId);
		}
	}

	@Override
	public boolean contains(final DbMosaicId dbMosaicId) {
		synchronized (this.lock) {
			return this.cache.contains(dbMosaicId);
		}
	}

	@Override
	public void add(final MosaicId mosaicId, final DbMosaicId dbMosaicId) {
		synchronized (this.lock) {
			this.cache.add(mosaicId, dbMosaicId);
		}
	}

	@Override
	public void remove(final MosaicId mosaicId) {
		synchronized (this.lock) {
			this.cache.remove(mosaicId);
		}
	}

	@Override
	public void remove(final DbMosaicId dbMosaicId) {
		synchronized (this.lock) {
			this.cache.remove(dbMosaicId);
		}
	}

	@Override
	public void clear() {
		synchronized (this.lock) {
			this.cache.clear();
		}
	}
}
