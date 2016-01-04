package org.nem.nis.cache;

public class SynchronizedNamespaceCacheTest extends NamespaceCacheTest<SynchronizedNamespaceCache> {

	@Override
	protected SynchronizedNamespaceCache createImmutableCache() {
		return new SynchronizedNamespaceCache(new DefaultNamespaceCache());
	}
}
