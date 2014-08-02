package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;

/**
 * An account input / output service.
 */
public interface AccountIo extends Iterable<Account> {

	/**
	 * Finds an account given an address.
	 *
	 * @param address The address.
	 * @return The account.
	 */
	Account findByAddress(final Address address);

	/**
	 * Gets all transaction information associated with an account.
	 *
	 * @param address The account address.
	 * @param timestamp The maximum timestamp of a transaction.
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address, final String timestamp);

	/**
	 * Gets information about blocks harvested by an account.
	 *
	 * @param address The account address.
	 * @param timestamp The maximum timestamp of a block.
	 * @return The information about harvested blocks.
	 */
	SerializableList<HarvestInfo> getAccountHarvests(final Address address, final String timestamp);
}
