package org.nem.nis.cache;

public class SynchronizedExpiredMosaicCacheTest extends ExpiredMosaicCacheTest<SynchronizedExpiredMosaicCache> {
	@Override
	protected SynchronizedExpiredMosaicCache createImmutableCache() {
		return new SynchronizedExpiredMosaicCache(new DefaultExpiredMosaicCache());
	}
}
