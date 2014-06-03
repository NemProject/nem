package org.nem.nis.controller.viewmodels;

/**
 * Builder that is used by Spring to create an AccountPage from a GET request.
 */
public class AccountPageBuilder {

	private String address;
	private String timestamp;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp The timestamp.
	 */
	public void setTimestamp(final String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Creates an AccountPage.
	 *
	 * @return The account page.
	 */
	public AccountPage build() {
		return new AccountPage(this.address, this.timestamp);
	}
}
