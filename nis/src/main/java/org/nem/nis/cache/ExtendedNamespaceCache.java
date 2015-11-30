package org.nem.nis.cache;

/**
 * All the interfaces that the DefaultNamespaceCache is expected to implement.
 */
public interface ExtendedNamespaceCache<T extends CopyableCache> extends
		NamespaceCache,
		CopyableCache<T>,
		CommittableCache {
}