package org.nem.nis.cache;

public class SynchronizedAccountCacheTest extends AccountCacheTest<SynchronizedAccountCache> {

	@Override
	protected SynchronizedAccountCache createAccountCache() {
		return new SynchronizedAccountCache(new DefaultAccountCache());
	}
}
