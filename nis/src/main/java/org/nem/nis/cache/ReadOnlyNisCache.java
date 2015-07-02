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
	ReadOnlyAccountCache getAccountCache();

	/**
	 * Gets the account state cache.
	 *
	 * @return The account state cache.
	 */
	ReadOnlyAccountStateCache getAccountStateCache();

	/**
	 * Gets the poi facade.
	 *
	 * @return The poi facade.
	 */
	ReadOnlyPoiFacade getPoiFacade();

	/**
	 * Gets the transaction hash cache.
	 *
	 * @return The transaction hash cache.
	 */
	ReadOnlyHashCache getTransactionHashCache();

	/**
	 * Gets the namespace cache.
	 *
	 * @return The namespace cache.
	 */
	ReadOnlyNamespaceCache getNamespaceCache();

	/**
	 * Gets the mosaic cache.
	 *
	 * @return The mosaic cache.
	 */
	ReadOnlyMosaicCache getMosaicCache();

	/**
	 * Creates a mutable copy of this NIS cache.
	 *
	 * @return The copy.
	 */
	NisCache copy();
}
