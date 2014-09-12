package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountPage from a GET request.
 */
public class AccountPageBuilder {

	private String address;
	private String timeStamp;

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
	 * @param timeStamp The timestamp.
	 */
	public void setTimeStamp(final String timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Creates an AccountPage.
	 *
	 * @return The account page.
	 */
	public AccountPage build() {
		return new AccountPage(this.address, this.timeStamp);
	}
}
