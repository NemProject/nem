package org.nem.core.model.ncc;

/**
 * Builder that is used by Spring to create an AccountId from a GET request.
 */
public class AccountIdBuilder {
	private String address;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Creates an AccountId.
	 *
	 * @return The account id.
	 */
	public AccountId build() {
		return new AccountId(this.address);
	}
}
