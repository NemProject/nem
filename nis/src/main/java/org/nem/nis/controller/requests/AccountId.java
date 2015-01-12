package org.nem.nis.controller.requests;

import org.nem.core.model.Address;
import org.nem.core.serialization.*;

/**
 * View model that represents an account id information.
 */
public class AccountId implements SerializableEntity {
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
		this.checkValidity();
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
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "account", this.address);
	}
}
