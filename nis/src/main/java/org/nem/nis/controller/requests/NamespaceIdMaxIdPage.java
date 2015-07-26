package org.nem.nis.controller.requests;

import org.nem.core.model.namespace.*;

/**
 * View model that represents a namespaceId-maxId page of information.
 */
public class NamespaceIdMaxIdPage extends DefaultPage {
	private final NamespaceId namespaceId;

	/**
	 * Creates a new page.
	 *
	 * @param id The id.
	 * @param pageSize The pageSize.
	 * @param namespace The namespace.
	 */
	public NamespaceIdMaxIdPage(final String id, final String pageSize, final String namespace) {
		super(id, pageSize);
		this.namespaceId = new NamespaceId(namespace);
	}

	/**
	 * Gets the namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.namespaceId;
	}
}
