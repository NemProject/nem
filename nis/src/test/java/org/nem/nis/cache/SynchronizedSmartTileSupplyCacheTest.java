package org.nem.nis.cache;

public class SynchronizedSmartTileSupplyCacheTest extends SmartTileSupplyCacheTest<SynchronizedSmartTileSupplyCache> {

	@Override
	protected SynchronizedSmartTileSupplyCache createCache() {
		return new SynchronizedSmartTileSupplyCache(new DefaultSmartTileSupplyCache());
	}
}
