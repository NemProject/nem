package org.nem.nis.cache;

/**
 * Cache that supports automatic caching of unknown entities.
 */
public interface AutoCache<T> {

	/**
	 * Creates an auto-caching cache.
	 *
	 * @return The auto-caching cache.
	 */
	public T asAutoCache();
}
