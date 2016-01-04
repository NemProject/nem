package org.nem.nis.controller.requests;

import org.nem.core.model.namespace.NamespaceId;

/**
 * Builder that is used by Spring to create a NamespaceId from a GET request.
 */
public class NamespaceIdBuilder {
	private String namespace;

	/**
	 * Sets the namespace.
	 *
	 * @param namespace The namespace.
	 */
	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Creates a NamespaceId.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId build() {
		return new NamespaceId(this.namespace);
	}
}
