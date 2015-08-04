package org.nem.nis.cache;

public class SynchronizedMosaicIdCacheTest extends MosaicIdCacheTest<SynchronizedMosaicIdCache> {

	@Override
	protected SynchronizedMosaicIdCache createCache() {
		return new SynchronizedMosaicIdCache(new DefaultMosaicIdCache());
	}
}
