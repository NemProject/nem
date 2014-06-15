package org.nem.nis.controller.viewmodels;

import org.nem.core.model.AccountImportance;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;

/**
 * A simple view model for account importance information.
 */
public class AccountImportanceViewModel implements SerializableEntity {

	private final Address address ;
	private final AccountImportance importance;

	/**
	 * Creates a new account importance view model.
	 *
	 * @param address The address.
	 * @param importance The importance.
	 */
	public AccountImportanceViewModel(final Address address, final AccountImportance importance) {
		this.address = address;
		this.importance = importance;
	}

	/**
	 * Deserializes an account importance view model.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountImportanceViewModel(final Deserializer deserializer) {
		this.address = Address.readFrom(deserializer, "address");
		this.importance = deserializer.readObject("importance", obj -> new AccountImportance(obj));
	}

	/**
	 * Gets the address.
	 *
	 * @return The address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the importance.
	 *
	 * @return The importance.
	 */
	public AccountImportance getImportance() {
		return this.importance;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "address", this.address);
		serializer.writeObject("importance", this.importance);
	}

	@Override
	public int hashCode() {
		return this.address.hashCode() ^ getHashCode(this.importance);
	}

	private static int getHashCode(final AccountImportance ai) {
		return ai.isSet() ? ai.getHeight().hashCode() : 0;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AccountImportanceViewModel))
			return false;

		final AccountImportanceViewModel rhs = (AccountImportanceViewModel)obj;
		return this.address.equals(rhs.address) && areImportancesEqual(this.importance, rhs.importance);
	}

	private static boolean areImportancesEqual(final AccountImportance lhs, final AccountImportance rhs) {
		if (!lhs.isSet() && !rhs.isSet())
			return true;

		if (!lhs.isSet() || !rhs.isSet())
			return false;

		return lhs.getHeight().equals(rhs.getHeight())
				&& lhs.getImportance(lhs.getHeight()) == rhs.getImportance(rhs.getHeight());
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.address, this.importance);
	}
}
