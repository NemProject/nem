package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;

import java.util.Collection;

/**
 * A read-only mapping of mosaic balances.
 */
@SuppressWarnings("unused")
public interface ReadOnlyMosaicBalances {

	/**
	 * Gets the number of accounts with non-zero balances.
	 *
	 * @return The number of accounts.
	 */
	int size();

	/**
	 * Gets the balance for the specified account. This function will return zero for unknown accounts.
	 *
	 * @param address The account address.
	 * @return The balance.
	 */
	Quantity getBalance(final Address address);

	/**
	 * Get the collection of owners addresses for the mosaic.
	 *
	 * @return The collection of owner addresses.
	 */
	Collection<Address> getOwners();
}
