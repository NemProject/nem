package org.nem.nis.cache;

public class DefaultNamespaceCacheTest extends NamespaceCacheTest<DefaultNamespaceCache> {

	@Override
	protected DefaultNamespaceCache createCache() {
		return new DefaultNamespaceCache();
	}
}
