package org.nem.nis.cache;

import org.nem.core.model.namespace.*;

/**
 * A readonly namespace cache.
 */
public interface ReadOnlyNamespaceCache {

	/**
	 * Gets the size of the underlying hash map.
	 *
	 * @return The size.
	 */
	int size();

	/**
	 * Gets a namespace object specified by its id.
	 *
	 * @param id The namespace id.
	 * @return The namespace object.
	 */
	Namespace get(final NamespaceId id);

	/**
	 * Returns a value indicating whether or not the cache contains a namespace object with the specified id.
	 *
	 * @param id The namespace id.
	 * @return true if a namespace with the specified id exists in the cache, false otherwise.
	 */
	boolean contains(final NamespaceId id);
}
