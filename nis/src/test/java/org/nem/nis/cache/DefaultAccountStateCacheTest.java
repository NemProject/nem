package org.nem.nis.cache;

public class DefaultAccountStateCacheTest extends AccountStateCacheTest<DefaultAccountStateCache> {

	@Override
	protected DefaultAccountStateCache createCacheWithoutAutoCache() {
		return new DefaultAccountStateCache();
	}
}
