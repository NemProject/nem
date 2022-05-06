package org.nem.nis.cache;

import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.ReadOnlyNamespaceEntry;

import java.util.Collection;

/**
 * A readonly namespace cache.
 */
public interface ReadOnlyNamespaceCache {

	/**
	 * Gets the number of unique namespaces in the cache.
	 *
	 * @return The size.
	 */
	int size();

	/**
	 * Gets the total number of unique namespaces in the cache (including versions).
	 *
	 * @return The size.
	 */
	int deepSize();

	/**
	 * Gets the collection of all root namespace ids.
	 *
	 * @return The collection of root namespace ids.
	 */
	Collection<NamespaceId> getRootNamespaceIds();

	/**
	 * Gets a namespace entry specified by its id.
	 *
	 * @param id The namespace id.
	 * @return The namespace entry.
	 */
	ReadOnlyNamespaceEntry get(final NamespaceId id);

	/**
	 * Gets the collection of all sub namespace ids for a given root namespace.
	 *
	 * @param rootId The root namespace id.
	 * @return The collection of sub namespace ids.
	 */
	Collection<NamespaceId> getSubNamespaceIds(final NamespaceId rootId);

	/**
	 * Returns a value indicating whether or not the cache contains a namespace object with the specified id.
	 *
	 * @param id The namespace id.
	 * @return true if a namespace with the specified id exists in the cache, false otherwise.
	 */
	boolean contains(final NamespaceId id);

	/**
	 * Gets a value indicating whether or not a given namespace is active at a given height.
	 *
	 * @param id The namespace id.
	 * @param height The height to test.
	 * @return True if the namespace is active at the given height, false otherwise.
	 */
	boolean isActive(final NamespaceId id, final BlockHeight height);
}
