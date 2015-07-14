package org.nem.nis.state;

import org.nem.core.model.namespace.Namespace;

/**
 * A writable namespace entry.
 */
public class NamespaceEntry implements ReadOnlyNamespaceEntry {

	@Override
	public Namespace getNamespace() {
		return null;
	}

	@Override
	public SmartTiles getSmartTiles() {
		return null;
	}
}
