package org.nem.nis.cache;

public class SynchronizedHashCacheTest extends HashCacheTest<SynchronizedHashCache> {

	@Override
	protected SynchronizedHashCache createWritableCache() {
		return new SynchronizedHashCache(new DefaultHashCache().copy());
	}

	@Override
	protected SynchronizedHashCache createReadOnlyCacheWithRetentionTime(final int retentionTime) {
		return new SynchronizedHashCache(new DefaultHashCache(50, retentionTime));
	}
}
