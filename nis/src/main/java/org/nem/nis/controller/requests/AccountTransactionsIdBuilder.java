package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountTransactionsId from a GET request.
 */
public class AccountTransactionsIdBuilder {
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
	 * Creates an AccountTransactionsId.
	 *
	 * @return The id.
	 */
	public AccountTransactionsId build() {
		return new AccountTransactionsId(this.address, this.hash);
	}
}
