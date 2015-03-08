package org.nem.core.serialization;

import org.nem.core.model.*;

/**
 * Class that contains external state necessary for deserialization of some objects.
 */
public class DeserializationContext extends SerializationContext {
	private final SimpleAccountLookup accountLookup;

	/**
	 * Creates a new DeserializationContext around the specified parameters.
	 *
	 * @param accountLookup The account lookup policy.
	 */
	public DeserializationContext(final SimpleAccountLookup accountLookup) {
		this.accountLookup = accountLookup;
	}

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 * @return The account with the specified id.
	 */
	public Account findAccountByAddress(final Address id) {
		return this.accountLookup.findByAddress(id);
	}
}
