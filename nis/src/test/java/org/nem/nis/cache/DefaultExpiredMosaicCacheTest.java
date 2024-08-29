package org.nem.nis.cache;

public class DefaultExpiredMosaicCacheTest extends ExpiredMosaicCacheTest<DefaultExpiredMosaicCache> {
	@Override
	protected DefaultExpiredMosaicCache createImmutableCache() {
		return new DefaultExpiredMosaicCache();
	}
}
