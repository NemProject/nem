package org.nem.nis.controller.viewmodels;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;

public class AccountTransactionsPage {

	private final Address address;
	private final Hash hash;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param hash The hash.
	 */
	public AccountTransactionsPage(final String address, final String hash) {
		if (null == address)
			throw new IllegalArgumentException("address is required");

		this.address = Address.fromEncoded(address);
		if (!this.address.isValid())
			throw new IllegalArgumentException("address must be valid");

		this.hash = (hash != null) ? Hash.fromHexString(hash) : null;
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
	 * Gets the hash.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}
}
