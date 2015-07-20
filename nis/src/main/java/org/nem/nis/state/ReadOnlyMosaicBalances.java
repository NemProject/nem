package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;

/**
 * A read-only mapping of mosaic balances.
 */
public interface ReadOnlyMosaicBalances {

	/**
	 * Gets the number of accounts with non-zero balances.
	 *
	 * @return The number of accounts.
	 */
	int size();

	/**
	 * Gets the balance for the specified account.
	 * This function will return zero for unknown accounts.
	 *
	 * @param address The account address.
	 * @return The balance.
	 */
	Quantity getBalance(final Address address);
}
