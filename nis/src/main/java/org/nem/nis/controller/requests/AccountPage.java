package org.nem.nis.controller.requests;

import org.nem.core.model.Address;

/**
 * View model that represents a page of account-related information.
 */
public class AccountPage {

	private final Address address;
	private final String timeStamp;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param timeStamp The timestamp.
	 */
	public AccountPage(final String address, final String timeStamp) {
		if (null == address) {
			throw new IllegalArgumentException("address is required");
		}

		this.address = Address.fromEncoded(address);
		if (!this.address.isValid()) {
			throw new IllegalArgumentException("address must be valid");
		}

		this.timeStamp = timeStamp;
	}

	/**
	 * Gets the address
	 *
	 * @return The address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return The timestamp.
	 */
	public String getTimeStamp() {
		return this.timeStamp;
	}
}
