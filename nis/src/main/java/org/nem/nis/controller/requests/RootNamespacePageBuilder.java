package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create a RootNamespacePage from a GET request.
 */
public class RootNamespacePageBuilder {
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
	 * Creates a RootNamespacePage.
	 *
	 * @return The root namespace page.
	 */
	public RootNamespacePage build() {
		return new RootNamespacePage(this.id, this.pageSize);
	}
}
