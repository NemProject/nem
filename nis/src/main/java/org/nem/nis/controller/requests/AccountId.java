package org.nem.nis.controller.requests;

import org.nem.core.model.Address;

/**
 * View model that represents an account id information.
 */
public class AccountId {
	private final Address address;

	/**
	 * Creates a new account id.
	 *
	 * @param address The address.
	 */
	public AccountId(final String address) {
		if (null == address) {
			throw new IllegalArgumentException("address is required");
		}

		this.address = Address.fromEncoded(address);
		if (!this.address.isValid()) {
			throw new IllegalArgumentException("address must be valid");
		}
	}

	/**
	 * Gets the address
	 *
	 * @return The address.
	 */
	public Address getAddress() {
		return this.address;
	}
}
