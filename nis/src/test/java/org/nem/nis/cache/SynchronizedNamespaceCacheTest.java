package org.nem.nis.cache;

public class SynchronizedNamespaceCacheTest extends NamespaceCacheTest<SynchronizedNamespaceCache> {

	@Override
	protected SynchronizedNamespaceCache createCache() {
		return new SynchronizedNamespaceCache(new DefaultNamespaceCache());
	}
}
