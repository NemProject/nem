package org.nem.nis.cache;

import org.nem.nis.ForkConfiguration;

public class DefaultNamespaceCacheTest extends NamespaceCacheTest<DefaultNamespaceCache> {

	@Override
	protected DefaultNamespaceCache createImmutableCache() {
		final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
		return new DefaultNamespaceCache(forkConfiguration.getMosaicRedefinitionForkHeight());
	}
}
