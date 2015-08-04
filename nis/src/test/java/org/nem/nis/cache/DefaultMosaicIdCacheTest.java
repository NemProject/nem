package org.nem.nis.cache;

public class DefaultMosaicIdCacheTest extends MosaicIdCacheTest<DefaultMosaicIdCache> {

	@Override
	protected DefaultMosaicIdCache createCache() {
		return new DefaultMosaicIdCache();
	}
}
