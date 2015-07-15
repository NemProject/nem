package org.nem.nis.state;

import org.nem.core.model.namespace.Namespace;

/**
 * A writable namespace entry.
 */
public class NamespaceEntry implements ReadOnlyNamespaceEntry {
	private final Namespace namespace;
	private final Mosaics mosaics;

	/**
	 * Creates a new namespace entry.
	 *
	 * @param namespace The namespace.
	 * @param mosaics The mosaics.
	 */
	public NamespaceEntry(final Namespace namespace, final Mosaics mosaics) {
		this.namespace = namespace;
		this.mosaics = mosaics;
	}

	@Override
	public Namespace getNamespace() {
		return this.namespace;
	}

	@Override
	public Mosaics getMosaics() {
		return this.mosaics;
	}

	/**
	 * Creates a copy of this entry.
	 *
	 * @return A copy of this entry.
	 */
	public NamespaceEntry copy() {
		return new NamespaceEntry(this.namespace, this.mosaics.copy());
	}
}
