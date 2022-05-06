package org.nem.nis.cache;

public class DefaultAccountCacheTest extends AccountCacheTest<DefaultAccountCache> {

	@Override
	protected DefaultAccountCache createAccountCache() {
		return new DefaultAccountCache();
	}
}
