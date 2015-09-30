package org.nem.nis.cache;

public class DefaultHashCacheTest extends HashCacheTest<DefaultHashCache> {

	@Override
	protected DefaultHashCache createWritableCache() {
		return new DefaultHashCache().copy();
	}

	@Override
	protected DefaultHashCache createWritableCacheWithRetentionTime(final int retentionTime) {
		return new DefaultHashCache(50, retentionTime).copy();
	}

	@Override
	protected DefaultHashCache createReadOnlyCacheWithRetentionTime(int retentionTime) {
		return new DefaultHashCache(50, retentionTime);
	}
}
