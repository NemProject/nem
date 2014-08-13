package org.nem.nis.controller.viewmodels;

import org.nem.core.model.Address;

/**
 * View model that represents a page of account-related information.
 */
public class AccountPage {

	private final Address address;
	private final String timestamp;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param timestamp The timestamp.
	 */
	public AccountPage(final String address, final String timestamp) {
		if (null == address) {
			throw new IllegalArgumentException("address is required");
		}

		this.address = Address.fromEncoded(address);
		if (!this.address.isValid()) {
			throw new IllegalArgumentException("address must be valid");
		}

		this.timestamp = timestamp;
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
	public String getTimestamp() {
		return this.timestamp;
	}
}
