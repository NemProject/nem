package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A namespace cache.
 */
public interface NamespaceCache extends ReadOnlyNamespaceCache {

	/**
	 * Adds a namespace object to the cache.
	 *
	 * @param namespace The namespace.
	 */
	void add(final Namespace namespace);

	/**
	 * Removes a namespace object from the cache.
	 *
	 * @param id The namespace id.
	 */
	void remove(final NamespaceId id);
}
