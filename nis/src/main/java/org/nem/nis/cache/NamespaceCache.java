package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.NamespaceEntry;

/**
 * A namespace cache.
 */
public interface NamespaceCache extends ReadOnlyNamespaceCache {

	/**
	 * Gets a namespace entry specified by its id.
	 *
	 * @param id The namespace id.
	 * @return The namespace entry.
	 */
	NamespaceEntry get(final NamespaceId id);

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

	/**
	 * Prunes the namespace cache given the current block height.
	 *
	 * @param height The current block height.
	 */
	void prune(final BlockHeight height);
}
