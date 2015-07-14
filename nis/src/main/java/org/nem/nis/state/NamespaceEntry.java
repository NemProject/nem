package org.nem.nis.state;

import org.nem.core.model.namespace.Namespace;

/**
 * A writable namespace entry.
 */
public class NamespaceEntry implements ReadOnlyNamespaceEntry {
	private final Namespace namespace;
	private final SmartTiles smartTiles;

	/**
	 * Creates a new namespace entry.
	 *
	 * @param namespace The namespace.
	 */
	public NamespaceEntry(final Namespace namespace) {
		this.namespace = namespace;
		this.smartTiles = new SmartTiles();
	}

	@Override
	public Namespace getNamespace() {
		return this.namespace;
	}

	@Override
	public SmartTiles getSmartTiles() {
		return this.smartTiles;
	}
}
