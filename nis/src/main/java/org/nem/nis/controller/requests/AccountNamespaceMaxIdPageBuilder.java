package org.nem.nis.controller.requests;

public class AccountNamespaceMaxIdPageBuilder {
	private String address;
	private String parent;
	private String id;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Sets the namespace.
	 *
	 * @param parent The namespace.
	 */
	public void setParent(final String parent) {
		this.parent = parent;
	}

	/**
	 * Sets the id.
	 *
	 * @param id The id.
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Creates an AccountNamespaceMaxIdPage.
	 *
	 * @return The account namespace maxId page.
	 */
	public AccountNamespaceMaxIdPage build() {
		return new AccountNamespaceMaxIdPage(this.address, this.parent, this.id);
	}
}
