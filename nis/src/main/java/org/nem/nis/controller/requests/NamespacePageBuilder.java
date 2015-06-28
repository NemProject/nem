package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create a NamespacePage from a GET request.
 */
public class NamespacePageBuilder {
	private String id;
	private String pageSize;

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
	 * @param pageSize The page size.
	 */
	public void setPageSize(final String pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Creates a NamespacePage.
	 *
	 * @return The root namespace page.
	 */
	public NamespacePage build() {
		return new NamespacePage(this.id, this.pageSize);
	}
}
