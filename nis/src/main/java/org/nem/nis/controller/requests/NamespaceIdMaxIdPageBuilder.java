package org.nem.nis.controller.requests;

public class NamespaceIdMaxIdPageBuilder {
	private String id;
	private String pageSize;
	private String namespace;

	/**
	 * Sets the id.
	 *
	 * @param id The id.
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Sets the page size.
	 *
	 * @param pageSize page size.
	 */
	public void setPageSize(final String pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Sets the namespace.
	 *
	 * @param namespace The namespace.
	 */
	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Creates an NamespaceIdMaxIdPage.
	 *
	 * @return The namespaceId-maxId page.
	 */
	public NamespaceIdMaxIdPage build() {
		return new NamespaceIdMaxIdPage(this.id, this.pageSize, this.namespace);
	}
}
