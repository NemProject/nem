package org.nem.nis.cache;

/**
 * All the interfaces that the DefaultAccountStateCache is expected to implement.
 */
public interface ExtendedAccountStateCache<T extends CopyableCache> extends
		AccountStateCache,
		CopyableCache<T>,
		CommittableCache,
		AutoCache<AccountStateCache> {
}