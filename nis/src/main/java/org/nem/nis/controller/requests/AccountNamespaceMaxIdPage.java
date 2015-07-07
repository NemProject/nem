package org.nem.nis.controller.requests;

/**
 * View model for requests of account-related mosaic information.
 */
public class AccountNamespaceMaxIdPage extends AccountNamespacePage {
	private final Long id;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param parent The namespace full qualified name.
	 * @param id The max. id.
	 */
	public AccountNamespaceMaxIdPage(final String address, final String parent, final String id) {
		super(address, parent);
		this.id = null == id ? null : Long.parseLong(id);
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}
}
