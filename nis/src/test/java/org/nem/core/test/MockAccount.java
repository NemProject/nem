package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.Account;
import org.nem.core.model.Address;

/**
 * A mock Account implementation.
 */
public class MockAccount extends Account {
	private final Address address;

	/**
	 * Creates a mock account around an address.
	 *
	 * @param address The mock account's address.
	 */
	public MockAccount(final Address address) {
		super(new KeyPair());
		this.address = address;
	}

	@Override
	public Address getAddress() {
		return this.address;
	}
}