package org.nem.nis.cache;

public class DefaultHashCacheTest extends HashCacheTest<DefaultHashCache> {

	@Override
	protected DefaultHashCache createCache() {
		return new DefaultHashCache();
	}

	@Override
	protected DefaultHashCache createCacheWithRetentionTime(final int retentionTime) {
		return new DefaultHashCache(50, retentionTime);
	}
}
