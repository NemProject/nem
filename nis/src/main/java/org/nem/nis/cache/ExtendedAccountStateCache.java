package org.nem.nis.cache;

/**
 * All the interfaces that the DefaultAccountStateCache is expected to implement.
 */
@SuppressWarnings("rawtypes")
public interface ExtendedAccountStateCache<T extends CopyableCache> extends AccountStateCache, CopyableCache<T>, CommittableCache {
}
