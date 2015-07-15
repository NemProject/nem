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
	 * @param tiles The smart tiles.
	 */
	public NamespaceEntry(final Namespace namespace, final SmartTiles tiles) {
		this.namespace = namespace;
		this.smartTiles = tiles;
	}

	@Override
	public Namespace getNamespace() {
		return this.namespace;
	}

	@Override
	public SmartTiles getSmartTiles() {
		return this.smartTiles;
	}

	/**
	 * Creates a copy of this entry.
	 *
	 * @return A copy of this entry.
	 */
	public NamespaceEntry copy() {
		return new NamespaceEntry(this.namespace, this.smartTiles.copy());
	}
}
