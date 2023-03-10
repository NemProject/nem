package org.nem.nis.cache;

import org.nem.nis.ForkConfiguration;

public class SynchronizedNamespaceCacheTest extends NamespaceCacheTest<SynchronizedNamespaceCache> {

	@Override
	protected SynchronizedNamespaceCache createImmutableCache() {
		final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
		return new SynchronizedNamespaceCache(new DefaultNamespaceCache(forkConfiguration.getMosaicRedefinitionForkHeight()));
	}
}
