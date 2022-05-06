package org.nem.core.serialization;

import org.nem.core.model.*;

import java.util.function.Predicate;

/**
 * An interface for looking up accounts.
 */
public interface AccountLookup extends SimpleAccountLookup {

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 * @param validator The validator for validating the address.
	 * @return The account with the specified id.
	 */
	Account findByAddress(final Address id, Predicate<Address> validator);

	/**
	 * Checks if an account is known.
	 *
	 * @param id The account id.
	 * @return true if the account is known, false if unknown.
	 */
	boolean isKnownAddress(final Address id);
}
