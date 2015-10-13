package org.nem.nis.cache;

public class DefaultNamespaceCacheTest extends NamespaceCacheTest<DefaultNamespaceCache> {

	@Override
	protected DefaultNamespaceCache createImmutableCache() {
		return new DefaultNamespaceCache();
	}
}
