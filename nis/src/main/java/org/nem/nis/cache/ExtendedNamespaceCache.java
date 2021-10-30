package org.nem.nis.cache;

/**
 * All the interfaces that the DefaultNamespaceCache is expected to implement.
 */
@SuppressWarnings("rawtypes")
public interface ExtendedNamespaceCache<T extends CopyableCache> extends NamespaceCache, CopyableCache<T>, CommittableCache {
}
