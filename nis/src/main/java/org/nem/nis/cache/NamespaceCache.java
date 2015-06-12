package org.nem.nis.cache;

import org.nem.core.model.namespace.*;

/**
 * A namespace cache.
 */
public interface NamespaceCache extends ReadOnlyNamespaceCache {

	/**
	 * puts a namespace object into the cache.
	 *
	 * @param namespace The namespace.
	 */
	void set(final Namespace namespace);
}
