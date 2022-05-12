package org.nem.core.serialization;

import org.nem.core.model.*;

/**
 * A simpler interface for looking up accounts.
 */
public interface SimpleAccountLookup {

	/**
	 * Looks up an account by its id.
	 *
	 * @param id The account id.
	 * @return The account with the specified id.
	 */
	Account findByAddress(final Address id);
}
