package org.nem.nis.state;

import org.nem.core.model.namespace.Namespace;

/**
 * A read-only namespace entry.
 */
public interface ReadOnlyNamespaceEntry {

	/**
	 * Gets the namespace.
	 *
	 * @return The namespace.
	 */
	Namespace getNamespace();

	/**
	 * Gets the mosaics associated with the namespace.
	 *
	 * @return The mosaics.
	 */
	ReadOnlyMosaics getMosaics();
}
