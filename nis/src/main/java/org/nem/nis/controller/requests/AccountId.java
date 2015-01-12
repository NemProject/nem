package org.nem.nis.controller.requests;

import org.nem.core.model.Address;
import org.nem.core.serialization.*;

/**
 * View model that represents an account id information.
 * TODO 20150112 J-B: i don't see where you need to support SerializableEntity
 * > afaikt, the only requirement is to be able to deserialize the request
 * > if so, i would drop 'implements SerializableEntity'
 * > the reason being that this is the root of a request hierarchy and it would be weird
 * > and possibly error prone if all of the classes were serializable but only partially
 * > ok, i see you ran into the generic bounds issue on the SerializableList<>; let's leave this
 * > for now, but it's probably something we should fix later
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

	//@Override
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "account", this.address);
	}
}
