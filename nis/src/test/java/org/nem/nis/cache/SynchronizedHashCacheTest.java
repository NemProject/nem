package org.nem.nis.cache;

public class SynchronizedHashCacheTest extends HashCacheTest<SynchronizedHashCache> {

	@Override
	protected SynchronizedHashCache createCache() {
		return new SynchronizedHashCache(new DefaultHashCache());
	}

	@Override
	protected SynchronizedHashCache createCacheWithRetentionTime(final int retentionTime) {
		return new SynchronizedHashCache(new DefaultHashCache(50, retentionTime));
	}
}