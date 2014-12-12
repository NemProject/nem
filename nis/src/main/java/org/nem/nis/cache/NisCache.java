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
	public AccountCache getAccountCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	public AccountStateRepository getAccountStateCache();

	/**
	 * Gets the poi facade.
	 *
	 * @return The poi facade.
	 */
	public PoiFacade getPoiFacade();

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	public HashCache getTransactionHashCache();

	/**
	 * Commits all changes to the "real" cache.
	 */
	public void commit();
}