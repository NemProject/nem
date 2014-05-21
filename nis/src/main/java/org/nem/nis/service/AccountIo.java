package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;

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
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address);

	/**
	 * Gets all blocks forged by an account.
	 *
	 * @param address The account address.
	 * @return The blocks.
	 */
	SerializableList<Block> getAccountBlocks(final Address address);
}
