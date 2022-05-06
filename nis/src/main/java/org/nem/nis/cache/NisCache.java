package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public interface NisCache extends ReadOnlyNisCache {

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	AccountCache getAccountCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	AccountStateCache getAccountStateCache();

	/**
	 * Gets the pox facade.
	 *
	 * @return The pox facade.
	 */
	PoxFacade getPoxFacade();

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	HashCache getTransactionHashCache();

	/**
	 * Gets the namespace cache.
	 *
	 * @return The namespace cache.
	 */
	NamespaceCache getNamespaceCache();

	/**
	 * Commits all changes to the "real" cache.
	 */
	void commit();
}
