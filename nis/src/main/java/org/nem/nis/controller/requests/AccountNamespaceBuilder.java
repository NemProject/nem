package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountNamespace from a GET request.
 */
public class AccountNamespaceBuilder {
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
	 * Creates an account namespace.
	 *
	 * @return The account namespace.
	 */
	public AccountNamespace build() {
		return new AccountNamespace(this.address, this.parent);
	}
}
