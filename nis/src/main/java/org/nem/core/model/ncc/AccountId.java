package org.nem.core.model.ncc;

import org.nem.core.model.Address;
import org.nem.core.serialization.Deserializer;

/**
 * View model that represents an account id information.
 */
public class AccountId {
	private final Address address;

	/**
	 * Creates a new account id.
	 *
	 * @param address The account address.
	 */
	public AccountId(final Address address) {
		if (null == address) {
			throw new IllegalArgumentException("address is required");
		}

		this.address = address;
		this.checkValidity();
	}

	/**
	 * Creates a new account id.
	 *
	 * @param address The encoded address.
	 */
	public AccountId(final String address) {
		this(Address.fromEncoded(address));
	}

	/**
	 * Creates a new account id.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountId(final Deserializer deserializer) {
		this.address = Address.readFrom(deserializer, "account");
		this.checkValidity();
	}

	private void checkValidity() {
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

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AccountId)) {
			return false;
		}

		final AccountId rhs = (AccountId)obj;
		return this.address.equals(rhs.address);
	}
}
