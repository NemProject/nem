package org.nem.nis.cache;

public class SynchronizedMosaicCacheTest extends MosaicCacheTest<SynchronizedMosaicCache> {

	@Override
	protected SynchronizedMosaicCache createCache() {
		return new SynchronizedMosaicCache(new DefaultMosaicCache());
	}
}
