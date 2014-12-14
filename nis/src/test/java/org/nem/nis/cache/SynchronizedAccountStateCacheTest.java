package org.nem.nis.cache;

public class SynchronizedAccountStateCacheTest extends AccountStateCacheTest<SynchronizedAccountStateCache> {

	@Override
	protected SynchronizedAccountStateCache createCache() {
		return new SynchronizedAccountStateCache(new DefaultAccountStateCache());
	}
}
