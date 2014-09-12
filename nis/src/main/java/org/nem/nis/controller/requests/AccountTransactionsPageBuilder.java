package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountTransactionsPage from a GET request.
 */
public class AccountTransactionsPageBuilder {

	private String address;
	private String hash;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Sets the hash.
	 *
	 * @param hash The hash.
	 */
	public void setHash(final String hash) {
		this.hash = hash;
	}

	/**
	 * Creates an AccountPage.
	 *
	 * @return The account page.
	 */
	public AccountTransactionsPage build() {
		return new AccountTransactionsPage(this.address, this.hash);
	}
}
