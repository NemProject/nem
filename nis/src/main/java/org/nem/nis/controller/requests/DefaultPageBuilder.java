package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create a DefaultPage from a GET request.
 */
public class DefaultPageBuilder {
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
	 * Creates a DefaultPage.
	 *
	 * @return The page.
	 */
	public DefaultPage build() {
		return new DefaultPage(this.id, this.pageSize);
	}
}
