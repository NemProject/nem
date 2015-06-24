package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountNamespacePage from a GET request.
 */
public class AccountNamespacePageBuilder {
	private String address;
	private String parent;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Sets the parent.
	 *
	 * @param parent The parent.
	 */
	public void setParent(final String parent) {
		this.parent = parent;
	}

	/**
	 * Creates an AccountNamespacePage.
	 *
	 * @return The account namespace page.
	 */
	public AccountNamespacePage build() {
		return new AccountNamespacePage(this.address, this.parent);
	}
}
