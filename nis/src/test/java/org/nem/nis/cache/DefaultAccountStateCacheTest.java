package org.nem.nis.cache;

public class DefaultAccountStateCacheTest extends AccountStateCacheTest<DefaultAccountStateCache> {

	@Override
	protected DefaultAccountStateCache createCache() {
		return new DefaultAccountStateCache();
	}
}
