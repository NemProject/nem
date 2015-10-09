package org.nem.nis.cache;

/**
 * All the interfaces that the DefaultAccountCache is expected to implement.
 */
public interface ExtendedAccountCache<T extends CopyableCache> extends
		AccountCache,
		CopyableCache<T>,
		CommittableCache,
		AutoCache<AccountCache> {
}
