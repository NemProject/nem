package org.nem.nis.cache;

public class DefaultMosaicCacheTest extends MosaicCacheTest<DefaultMosaicCache> {

	@Override
	protected DefaultMosaicCache createCache() {
		return new DefaultMosaicCache();
	}
}
