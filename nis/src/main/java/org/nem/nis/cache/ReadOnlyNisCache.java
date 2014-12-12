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
	public AccountCache getAccountCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	public ReadOnlyAccountStateCache getAccountStateCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	public ReadOnlyPoiFacade getPoiFacade(); // TODO 20141212 fix me!

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	public HashCache getTransactionHashCache();

	/**
	 * Creates a mutable copy of this NIS cache.
	 *
	 * @return The copy.
	 */
	public NisCache copy();
}
