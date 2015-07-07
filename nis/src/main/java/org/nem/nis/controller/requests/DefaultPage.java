package org.nem.nis.controller.requests;

/**
 * View model that represents a page of information.
 */
public class DefaultPage {
	private static final int DEFAULT_PAGE_SIZE = 25;
	private static final int MIN_PAGE_SIZE = 5;
	private static final int MAX_PAGE_SIZE = 100;

	private final Long id;
	private final int pageSize;

	/**
	 * Creates a new page.
	 *
	 * @param id The id.
	 * @param pageSize The pageSize.
	 */
	public DefaultPage(final String id, final String pageSize) {
		this.id = null == id ? null : Long.parseLong(id);

		final int parsedPageSize = null == pageSize ? DEFAULT_PAGE_SIZE : Short.parseShort(pageSize);
		this.pageSize = Math.max(MIN_PAGE_SIZE, Math.min(MAX_PAGE_SIZE, parsedPageSize));
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the page size.
	 *
	 * @return The page size.
	 */
	public int getPageSize() {
		return this.pageSize;
	}
}
