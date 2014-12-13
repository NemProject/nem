package org.nem.nis.cache;

/**
 * The central point for accessing NIS data.
 */
public interface ReadOnlyNisCache {

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	public ReadOnlyAccountCache getAccountCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	public ReadOnlyAccountStateCache getAccountStateCache();

	/**
	 * Gets the poi facade.
	 *
	 * @return The poi facade.
	 */
	public ReadOnlyPoiFacade getPoiFacade();

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	public ReadOnlyHashCache getTransactionHashCache();

	/**
	 * Creates a mutable copy of this NIS cache.
	 *
	 * @return The copy.
	 */
	public NisCache copy();
}
