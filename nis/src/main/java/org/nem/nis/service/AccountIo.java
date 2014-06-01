package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.serialization.SerializableList;

/**
 * An account input / output service.
 */
public interface AccountIo {

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
	 * Gets all blocks forged by an account.
	 *
	 * @param address The account address.
	 * @param timestamp The maximum timestamp of a block.
	 * @return The blocks.
	 */
	SerializableList<Block> getAccountBlocks(final Address address, final String timestamp);
}
