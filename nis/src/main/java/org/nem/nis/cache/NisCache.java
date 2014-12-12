package org.nem.nis.cache;

/**
 * The central point for accessing NIS-mutable data.
 */
public interface NisCache {

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	public AccountCache getAccountCache();

	/**
	 * Gets the poi facade.
	 *
	 * @return The poi facade.
	 */
	public AccountStateRepository getPoiFacade();

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	public HashCache getTransactionHashCache();

	/**
	 * Gets a read-only representation of this cache.
	 *
	 * @return The read-only representation.
	 */
	public ReadOnlyNisCache asReadOnly();


	/**
	 * Commits all changes to the "real" cache.
	 */
	public void commit();
}