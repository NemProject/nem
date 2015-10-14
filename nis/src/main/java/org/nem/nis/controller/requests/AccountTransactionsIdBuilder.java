package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountTransactionsId from a GET request.
 */
public class AccountTransactionsIdBuilder {
	private String address;
	private String hash;
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
	 * Sets the hash.
	 *
	 * @param hash The hash.
	 */
	public void setHash(final String hash) {
		this.hash = hash;
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
	 * Creates an AccountTransactionsId.
	 *
	 * @return The account page.
	 */
	public AccountTransactionsId build() {
		return new AccountTransactionsId(this.address, this.hash, this.id);
	}
}
