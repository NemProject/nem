package org.nem.nis.cache;

public class SynchronizedAccountStateCacheTest extends AccountStateCacheTest<SynchronizedAccountStateCache> {

	@Override
	protected SynchronizedAccountStateCache createCacheWithoutAutoCache() {
		return new SynchronizedAccountStateCache(new DefaultAccountStateCache());
	}
}
